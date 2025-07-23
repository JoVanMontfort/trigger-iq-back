package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RedditAuthService {

    private static final Logger logger = LoggerFactory.getLogger(RedditAuthService.class);

    @Value("${reddit.client-id}")
    private String clientId;

    @Value("${reddit.client-secret}")
    private String clientSecret;

    @Value("${reddit.username}")
    private String username;

    @Value("${reddit.password}")
    private String password;

    @Value("${reddit.user-agent}")
    private String userAgent;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", userAgent);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://www.reddit.com/api/v1/access_token",
                request,
                JsonNode.class
        );

        // Check if the response body is null or does not contain the expected "access_token" field
        JsonNode responseBody = response.getBody();
        if (responseBody != null && responseBody.has("access_token")) {
            return responseBody.get("access_token").asText();
        } else {
            // Log or throw an exception if access_token is missing
            logger.error("Access token not found in response: " + (responseBody != null ? responseBody.toString() : "null"));
            throw new IllegalStateException("Access token is missing in the Reddit response.");
        }
    }
}