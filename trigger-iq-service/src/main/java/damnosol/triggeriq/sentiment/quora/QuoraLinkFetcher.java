package damnosol.triggeriq.sentiment.quora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class QuoraLinkFetcher {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.cx}")
    private String searchEngineId;

    public List<String> fetchQuoraLinks(String query) {
        try {
            String encodedQuery = URLEncoder.encode("site:quora.com " + query, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://www.googleapis.com/customsearch/v1?q=%s&key=%s&cx=%s",
                    encodedQuery, apiKey, searchEngineId
            );

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseStream);

            List<String> links = new ArrayList<>();
            if (root.has("items")) {
                for (JsonNode item : root.get("items")) {
                    links.add(item.get("link").asText());
                }
            }
            return links;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
