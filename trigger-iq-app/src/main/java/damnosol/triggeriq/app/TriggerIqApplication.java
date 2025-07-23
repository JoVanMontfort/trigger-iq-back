package damnosol.triggeriq.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import damnosol.triggeriq.sentiment.SentimentAnalyzer;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.reddit.RedditFetcher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = {"damnosol.triggeriq.sentiment.reddit", "damnosol.triggeriq.sentiment", "damnosol.triggeriq.config"})
public class TriggerIqApplication {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Register the JavaTimeModule to handle Java 8 Date/Time types like Instant
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) {
        SpringApplication.run(TriggerIqApplication.class, args);
    }

    @Bean
    CommandLineRunner run(RedditFetcher fetcher, SentimentAnalyzer analyzer) {
        return args -> {
            List<Post> fetchedPosts = fetcher.fetchTopPosts("technology", 5);
            String jsonPosts = objectMapper.writeValueAsString(fetchedPosts);
            List<Post> posts = objectMapper.readValue(jsonPosts, objectMapper.getTypeFactory().constructCollectionType(List.class, Post.class));
            analyzer.analyzeAndPrint(fetchedPosts); // No need for JSON conversion
        };
    }
}
