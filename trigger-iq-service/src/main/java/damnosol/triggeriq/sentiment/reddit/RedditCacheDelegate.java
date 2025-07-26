package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedditCacheDelegate {

    private static final Logger logger = LoggerFactory.getLogger(RedditCacheDelegate.class);

    private final ObjectMapper mapper;

    public RedditCacheDelegate(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Cacheable(value = "redditPosts", keyGenerator = "safeKeyGenerator")
    public List<Post> fetchTopPosts(String url, HttpEntity<String> entity) {
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
                String subreddit = data.path("subreddit").asText();

                // Just create empty comment list for now, filled later
                results.add(new Post(title, "Neutral", upvotes, date, new ArrayList<>(), subreddit, postId));
            }
        } catch (Exception e) {
            logger.error("Error fetching/parsing Reddit post data: {}", e.getMessage(), e);
        }

        return results;
    }

    @Cacheable(value = "redditComments", key = "#postUrl")
    public List<Comment> fetchComments(String postUrl, HttpEntity<String> entity) {
        String url = "https://www.reddit.com" + postUrl + ".json";
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

                comments.add(new Comment(text, "Neutral", upvotes, author, date));
            }
        } catch (Exception e) {
            logger.error("Error fetching Reddit comments: {}", e.getMessage(), e);
        }

        return comments;
    }
}