package damnosol.triggeriq.sentiment.quora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoraLinkFetcher {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.cx}")
    private String searchEngineId;

    private final StringRedisTemplate redisTemplate;

    public List<String> fetchQuoraLinks(String query) {
        try {
            log.info("Starting Quora link fetch for query: '{}'", query);

            String encodedQuery = URLEncoder.encode("site:quora.com " + query, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://www.googleapis.com/customsearch/v1?q=%s&key=%s&cx=%s",
                    encodedQuery, apiKey, searchEngineId
            );
            log.debug("Google CSE request URL: {}", url);

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            log.info("Google CSE API response code: {}", code);

            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseStream);

            List<String> links = new ArrayList<>();
            if (root.has("items")) {
                for (JsonNode item : root.get("items")) {
                    String link = item.get("link").asText();
                    links.add(link);
                    log.debug("Found Quora link: {}", link);
                }
            }

            log.info("Total Quora links fetched: {}", links.size());

            // Push links to Redis for async processing
            if (!links.isEmpty()) {
                Long pushedCount = redisTemplate.opsForList().leftPushAll("quora:pending", links);
                log.info("Pushed {} links into Redis queue 'quora:pending'",
                        pushedCount == null ? 0 : pushedCount);
            } else {
                log.warn("No links fetched for query, skipping Redis push.");
            }


            return links;
        } catch (Exception e) {
            log.error("Error fetching Quora links for query: {}", query, e);
            return Collections.emptyList();
        }
    }
}