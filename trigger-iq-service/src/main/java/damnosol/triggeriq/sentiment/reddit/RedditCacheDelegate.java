package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class RedditCacheDelegate {

    private static final Logger logger = LoggerFactory.getLogger(RedditCacheDelegate.class);

    private final ObjectMapper mapper;
    private final RedisTemplate<String, List<Post>> postListRedisTemplate;
    private final RedisTemplate<String, List<Comment>> commentListRedisTemplate;

    public RedditCacheDelegate(ObjectMapper mapper,
                               @Qualifier("postListRedisTemplate") RedisTemplate<String, List<Post>> postListRedisTemplate,
                               @Qualifier("commentListRedisTemplate") RedisTemplate<String, List<Comment>> commentListRedisTemplate) {
        this.mapper = mapper;
        this.postListRedisTemplate = postListRedisTemplate;
        this.commentListRedisTemplate = commentListRedisTemplate;
    }

    public List<Post> fetchPosts(String url, HttpEntity<String> entity) {
        String cacheKey = "reddit:posts:" + url;
        List<Post> cached = postListRedisTemplate.opsForValue().get(cacheKey);

        if (cached != null && !cached.isEmpty()) {
            logger.info("✅ Loaded Reddit posts from Redis cache: {}", cacheKey);
            return cached;
        }

        logger.info("📡 Fetching Reddit posts from network for: {}", url);
        List<Post> results = new ArrayList<>();

        try {
            ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
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

                results.add(new Post(title, "Neutral", upvotes, date, new ArrayList<>(), subreddit, postId));
            }

            // Save to Redis
            postListRedisTemplate.opsForValue().set(cacheKey, results, Duration.ofHours(8));
            logger.info("💾 Stored Reddit posts in Redis cache: {}", cacheKey);

        } catch (Exception e) {
            logger.error("❌ Error fetching/parsing Reddit post data: {}", e.getMessage(), e);
        }

        return results;
    }

    public List<Comment> fetchComments(String postUrl, HttpEntity<String> entity) {
        String redisKey = "reddit:comments:" + postUrl;

        List<Comment> cached = commentListRedisTemplate.opsForValue().get(redisKey);
        if (cached != null && !cached.isEmpty()) {
            logger.info("✅ Cache hit for comments: {}", postUrl);
            return cached;
        }

        logger.info("📡 Fetching fresh comments for: {}", postUrl);
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

            // ✅ Cache it
            commentListRedisTemplate.opsForValue().set(redisKey, comments, Duration.ofHours(8));

        } catch (Exception e) {
            logger.error("Error fetching Reddit comments: {}", e.getMessage(), e);
        }

        return comments;
    }
}