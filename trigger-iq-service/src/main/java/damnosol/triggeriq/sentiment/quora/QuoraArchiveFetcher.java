package damnosol.triggeriq.sentiment.quora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String WAYBACK_API = "https://archive.org/wayback/available?url=%s";
    private static final String SAVE_PAGE_NOW = "https://web.archive.org/save/%s";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Fetch an archived snapshot of a Quora URL if available,
     * otherwise attempt to trigger Archive.org to save it.
     */
    public Optional<String> fetchArchivedSnapshot(String quoraUrl) {
        try {
            log.info("Fetching archived snapshot for Quora URL: {}", quoraUrl);

            String encodedUrl = URLEncoder.encode(quoraUrl, StandardCharsets.UTF_8);
            String requestUrl = String.format(WAYBACK_API, encodedUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Wayback API response code: {}, body length: {}",
                    response.statusCode(), response.body().length());

            if (response.statusCode() != 200) {
                log.warn("Non-200 response from Wayback API for {}: {}", quoraUrl, response.statusCode());
                return Optional.empty();
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode snapshotNode = root.path("archived_snapshots").path("closest").path("url");

            if (!snapshotNode.isMissingNode()) {
                String snapshotUrl = snapshotNode.asText();
                log.info("Found archived snapshot for {}: {}", quoraUrl, snapshotUrl);
                return Optional.of(snapshotUrl);
            }

            log.warn("No archived snapshot found for {} — triggering SavePageNow", quoraUrl);
            triggerSavePageNow(quoraUrl);

        } catch (Exception e) {
            log.error("Error fetching archive for {}: {}", quoraUrl, e.getMessage(), e);
        }
        return Optional.empty();
    }

    private void triggerSavePageNow(String url) {
        try {
            String saveUrl = String.format(SAVE_PAGE_NOW, url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(saveUrl))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Triggered SavePageNow for {}, response code: {}", url, response.statusCode());

        } catch (Exception e) {
            log.error("Error calling SavePageNow for {}: {}", url, e.getMessage(), e);
        }
    }
}
