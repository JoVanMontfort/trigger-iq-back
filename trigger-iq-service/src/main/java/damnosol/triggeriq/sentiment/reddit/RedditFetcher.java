package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedditFetcher {

    private static final Logger logger = LoggerFactory.getLogger(RedditFetcher.class);

    private final RedditAuthService authService;
    private final ObjectMapper mapper;

    public RedditFetcher(RedditAuthService authService, ObjectMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    // Fetch the top posts with their comments and use Redis caching
    @Cacheable(value = "redditPosts", keyGenerator = "safeKeyGenerator") // Cache posts based on subreddit and limit
    public List<Post> fetchTopPosts(String subreddit, int limit) {
        String accessToken = authService.getAccessToken();
        String url = String.format("https://oauth.reddit.com/r/%s/hot.json?limit=%d", subreddit, limit);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);

        List<Post> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode posts = root.path("data").path("children");

            for (JsonNode post : posts) {
                JsonNode data = post.path("data");

                String postId = data.path("id").asText();
                String title = data.path("title").asText();
                int upvotes = data.path("score").asInt();
                long createdUtc = data.path("created_utc").asLong();
                OffsetDateTime date = Instant.ofEpochSecond(createdUtc).atOffset(ZoneOffset.UTC);
                String id = data.path("id").asText(); // Reddit post ID
                String subredditName = data.path("subreddit").asText(); // Extract subreddit name

                List<Comment> comments = fetchComments(data.path("permalink").asText());

                Post newPost = new Post(title, "Neutral", upvotes, date, comments, subredditName, id);
                results.add(newPost);
            }
        } catch (Exception e) {
            logger.error("Error fetching/parsing Reddit data: {}", e.getMessage(), e);
        }

        return results;
    }

    // Fetch comments for a post based on its permalink
    @Cacheable(value = "redditComments", key = "#postUrl")
    public List<Comment> fetchComments(String postUrl) {
        String url = "https://www.reddit.com" + postUrl + ".json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);

        List<Comment> comments = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode commentData = root.path(1).path("data").path("children");

            for (JsonNode commentNode : commentData) {
                JsonNode data = commentNode.path("data");

                String text = data.path("body").asText();
                double timestamp = data.path("created_utc").asDouble();
                OffsetDateTime date = Instant.ofEpochSecond((long) timestamp).atOffset(ZoneOffset.UTC);

                String author = data.path("author").asText();
                int upvotes = data.path("score").asInt();
                String sentiment = "Neutral";

                comments.add(new Comment(text, sentiment, upvotes, author, date));
            }
        } catch (Exception e) {
            logger.error("Error fetching comments: " + e.getMessage(), e);
        }

        return comments;
    }
}