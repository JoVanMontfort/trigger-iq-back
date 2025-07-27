package damnosol.triggeriq.sentiment.dto;

import damnosol.triggeriq.sentiment.reddit.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentAnalysisResult {

    // General Info
    private int totalPostsAnalyzed;
    private int totalComments;
    private List<String> subreddits;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private List<String> keywordsMatched;
    private List<String> authorsMatched;

    // Sentiment Breakdown
    private double averageSentimentScore;
    private Map<String, Integer> sentimentDistribution; // e.g. {"Positive": 10, "Neutral": 5, "Negative": 8}
    private Post mostPositivePost;
    private Post mostNegativePost;

    // Correlations
    private Double correlationSentimentUpvotes;
    private Double correlationLengthSentiment;

    // Model Analysis
    private boolean modelTrainingSuccess;
    private String modelUsed;
    private Double rSquared;
    private Map<String, Double> featureImportance;
    private List<String> predictionSamples;

    // NLP Insights
    private List<String> topKeywordsBySentiment;
    private List<String> frequentTerms;

    // Warnings or Notes
    private List<String> warnings;

}