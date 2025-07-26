package damnosol.triggeriq.sentiment.reddit;

import damnosol.triggeriq.sentiment.SentimentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SentimentAnalysisService {

    private final SentimentAnalyzer analyzer;

    public SentimentAnalysisService(SentimentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void analyze(List<Post> posts) {
        analyzer.analyze(posts);
    }

}