package damnosol.triggeriq.app;

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

    public static void main(String[] args) {
        SpringApplication.run(TriggerIqApplication.class, args);
    }

    @Bean
    CommandLineRunner run(RedditFetcher fetcher, SentimentAnalyzer analyzer) {
        return args -> {
            List<Post> fetchedPosts = fetcher.fetchTopPosts("RoastMe", 50 );
            analyzer.analyzeAndPrintAndStore(fetchedPosts); // No need for JSON conversion
        };
    }
}
