package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedditFetcher {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // Fetch the top posts with their comments, managing rate limit
    public List<Post> fetchTopPosts(String subreddit, int limit) {
        String url = "https://www.reddit.com/r/" + subreddit + "/hot.json?limit=" + limit;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<Post> results = new ArrayList<>();
        int retries = 0;
        final int MAX_RETRIES = 5;

        while (retries < MAX_RETRIES) {
            try {
                // Send the request and get the response
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                // Process the response body to extract post data
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode posts = root.path("data").path("children");

                for (JsonNode post : posts) {
                    String title = post.path("data").path("title").asText();
                    Instant date = Instant.ofEpochSecond(post.path("data").path("created_utc").asLong());
                    List<Comment> comments = fetchComments(post.path("data").path("permalink").asText());

                    // Default sentiment to "Neutral"
                    Post newPost = new Post(title, "Neutral", date, comments);
                    results.add(newPost);
                }

                break; // Break out of the retry loop if successful

            } catch (HttpClientErrorException.TooManyRequests e) {
                // Rate-limited by Reddit
                retries++;
                HttpHeaders responseHeaders = e.getResponseHeaders();
                String remainingRequests = responseHeaders.getFirst("x-ratelimit-remaining");
                String resetTime = responseHeaders.getFirst("x-ratelimit-reset");

                // Log rate limit information
                System.out.println("Rate limit hit. Remaining requests: " + remainingRequests);
                System.out.println("Rate limit reset time: " + resetTime);

                long resetTimeMillis = Long.parseLong(resetTime) * 1000; // Convert to milliseconds
                long waitTime = resetTimeMillis - System.currentTimeMillis();

                if (waitTime > 0) {
                    // Sleep until the reset time
                    System.out.println("Sleeping for " + waitTime + " ms until rate limit reset.");
                    try {
                        TimeUnit.MILLISECONDS.sleep(waitTime); // Wait until reset
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // Exponential backoff if reset time is already in the past
                    int exponentialBackoffTime = (int) Math.pow(2, retries) * 1000;
                    System.out.println("Retrying in " + exponentialBackoffTime + " ms...");
                    try {
                        TimeUnit.MILLISECONDS.sleep(exponentialBackoffTime);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                // Log or handle exception
                System.err.println("Error fetching/parsing Reddit data: " + e.getMessage());
                break; // Exit the loop on any other exception
            }
        }

        return results;
    }

    // Fetch comments for a post based on its permalink
    private List<Comment> fetchComments(String permalink) {
        String url = "https://www.reddit.com" + permalink + ".json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        List<Comment> comments = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode commentData = root.get(1).path("data").path("children");

            for (JsonNode commentNode : commentData) {
                String commentText = commentNode.path("data").path("body").asText();
                String author = commentNode.path("data").path("author").asText();
                Instant commentDate = Instant.ofEpochSecond(commentNode.path("data").path("created_utc").asLong());
                String sentiment = "Neutral";  // Default sentiment to "Neutral"
                comments.add(new Comment(commentText, sentiment, author, commentDate.toString()));
            }
        } catch (Exception e) {
            System.err.println("Error fetching comments: " + e.getMessage());
        }

        return comments;
    }
}