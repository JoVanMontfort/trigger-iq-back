package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoraArchiveFetcher {

    private static final String ARCHIVE_PH_SEARCH = "https://archive.ph/search/?q=%s";
    private static final String ARCHIVE_PH_SUBMIT = "https://archive.ph/submit/";

    private final HttpClient httpClient;

    public QuoraArchiveFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch or create an archive.ph snapshot for a Quora URL.
     */
    public Optional<String> fetchArchivedSnapshot(String quoraUrl) {
        try {
            return findSnapshot(quoraUrl)
                    .or(() -> submitSnapshot(quoraUrl));
        } catch (Exception e) {
            log.error("Failed to fetch archive for {}: {}", quoraUrl, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Search archive.ph for an existing snapshot.
     */
    private Optional<String> findSnapshot(String url) {
        try {
            String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
            String searchUrl = String.format(ARCHIVE_PH_SEARCH, encodedUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Non-200 from archive.ph search for {}: {}", url, response.statusCode());
                return Optional.empty();
            }

            // archive.ph returns HTML → very simple heuristic: first "https://archive.ph/<id>" link
            String body = response.body();
            int idx = body.indexOf("https://archive.ph/");
            if (idx > 0) {
                int end = body.indexOf("\"", idx);
                String snapshotUrl = body.substring(idx, end);
                log.info("Found archive.ph snapshot for {} → {}", url, snapshotUrl);
                return Optional.of(snapshotUrl);
            }

            log.info("No archive.ph snapshot found for {}", url);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error searching archive.ph for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Submit a new snapshot to archive.ph.
     */
    private Optional<String> submitSnapshot(String url) {
        try {
            String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
            String submitUrl = ARCHIVE_PH_SUBMIT + "?url=" + encodedUrl;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(submitUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                log.warn("Rate limited by archive.ph while submitting {}. Retrying later.", url);
                // enqueue for retry in your QuoraRetryScheduler
                return Optional.empty();
            }

            if (response.statusCode() == 200) {
                String body = response.body();
                int idx = body.indexOf("https://archive.ph/");
                if (idx > 0) {
                    int end = body.indexOf("\"", idx);
                    String snapshotUrl = body.substring(idx, end);
                    log.info("Created new archive.ph snapshot for {} → {}", url, snapshotUrl);
                    return Optional.of(snapshotUrl);
                }
            }

            log.warn("Failed to submit {} to archive.ph, status {}", url, response.statusCode());
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error submitting {} to archive.ph: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}