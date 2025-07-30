package damnosol.triggeriq.sentiment.quora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class QuoraArchiveFetcher {

    private static final String WAYBACK_API = "https://archive.org/wayback/available?url=%s";

    public Optional<String> fetchArchivedSnapshot(String quoraUrl) {
        try {
            String encodedUrl = URLEncoder.encode(quoraUrl, StandardCharsets.UTF_8);
            String requestUrl = String.format(WAYBACK_API, encodedUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(connection.getInputStream());

            JsonNode snapshot = root.path("archived_snapshots").path("closest").path("url");
            if (!snapshot.isMissingNode()) {
                return Optional.of(snapshot.asText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}