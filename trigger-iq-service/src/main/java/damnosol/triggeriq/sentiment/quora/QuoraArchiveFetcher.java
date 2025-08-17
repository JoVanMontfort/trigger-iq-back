package damnosol.triggeriq.sentiment.quora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
public class QuoraArchiveFetcher {

    private static final String WAYBACK_API = "https://archive.org/wayback/available?url=%s";
    private static final String SAVE_PAGE_NOW = "https://web.archive.org/save/%s";

    public Optional<String> fetchArchivedSnapshot(String quoraUrl) {
        try {
            log.info("Fetching archived snapshot for Quora URL: {}", quoraUrl);

            String encodedUrl = URLEncoder.encode(quoraUrl, StandardCharsets.UTF_8);
            String requestUrl = String.format(WAYBACK_API, encodedUrl);
            log.debug("Wayback API request URL: {}", requestUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            log.info("Wayback API response code: {}", code);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(connection.getInputStream());

            JsonNode snapshot = root.path("archived_snapshots").path("closest").path("url");
            if (!snapshot.isMissingNode()) {
                return Optional.of(snapshot.asText());
            }

            log.warn("No archived snapshot found for URL: {} — triggering SavePageNow", quoraUrl);
            triggerSavePageNow(quoraUrl);

        } catch (Exception e) {
            log.error("Error checking archive for URL: {}", quoraUrl, e);
        }
        return Optional.empty();
    }

    private void triggerSavePageNow(String url) {
        try {
            String saveUrl = String.format(SAVE_PAGE_NOW, url);
            log.debug("SavePageNow request: {}", saveUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(saveUrl).openConnection();
            connection.setRequestMethod("GET");
            log.info("SavePageNow response: {}", connection.getResponseCode());
        } catch (Exception e) {
            log.error("Error calling SavePageNow for {}", url, e);
        }
    }
}