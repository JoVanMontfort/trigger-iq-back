package damnosol.triggeriq.sentiment.reddit;

import damnosol.triggeriq.sentiment.dto.SentimentAnalysisResult;
import damnosol.triggeriq.sentiment.RedditPostsSentimentAnalyzer;
import damnosol.triggeriq.sentiment.result.AnalysisResult;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SentimentAnalysisService {

    private final RedditPostsSentimentAnalyzer sentimentAnalyzer;

    public SentimentAnalysisService(RedditPostsSentimentAnalyzer sentimentAnalyzer) {
        this.sentimentAnalyzer = sentimentAnalyzer;
    }

    public SentimentAnalysisResult analyze(List<Post> posts) {
        AnalysisResult analysisResult = sentimentAnalyzer.analyze(posts);

        List<Post> analyzedPosts = analysisResult.getPosts();
        Map<String, Integer> sentimentDistribution = getSentimentCounts(analyzedPosts);
        double averageSentimentScore = calculateAverageSentimentScore(analyzedPosts);
        Post mostPositivePost = findMostPositivePost(analyzedPosts);
        Post mostNegativePost = findMostNegativePost(analyzedPosts);

        return SentimentAnalysisResult.builder()
                .totalPostsAnalyzed(analyzedPosts.size())
                .totalComments(analyzedPosts.stream().mapToInt(p -> p.getComments().size()).sum())
                .subreddits(extractSubreddits(analyzedPosts))
                .dateFrom(getEarliestPostDate(analyzedPosts))
                .dateTo(getLatestPostDate(analyzedPosts))
                .keywordsMatched(getMatchedKeywords(analyzedPosts))
                .authorsMatched(getMatchedAuthors(analyzedPosts))
                .averageSentimentScore(averageSentimentScore)
                .sentimentDistribution(sentimentDistribution)
                .mostPositivePost(mostPositivePost)
                .mostNegativePost(mostNegativePost)
                .correlationSentimentUpvotes(calculateCorrelationSentimentUpvotes(analyzedPosts))
                .correlationLengthSentiment(calculateCorrelationLengthSentiment(analyzedPosts))
                .modelTrainingSuccess(analysisResult.isTrained())
                .modelUsed(analysisResult.getModelUsed())
                .rSquared(analysisResult.getRSquared())
                .featureImportance(analysisResult.getFeatureImportance())
                .predictionSamples(analysisResult.getPredictionSamples())
                .topKeywordsBySentiment(topKeywordsBySentiment(analyzedPosts))
                .frequentTerms(frequentTerms(analyzedPosts))
                .warnings(generateWarnings(analyzedPosts, analysisResult.isTrained()))
                .build();
    }

    private Map<String, Integer> getSentimentCounts(List<Post> posts) {
        return posts.stream()
                .flatMap(post -> post.getComments().stream())
                .collect(Collectors.groupingBy(Comment::getSentiment, Collectors.summingInt(c -> 1)));
    }

    private double calculateAverageSentimentScore(List<Post> posts) {
        return posts.stream()
                .flatMap(post -> post.getComments().stream())
                .mapToDouble(Comment::getSentimentScore)
                .average()
                .orElse(0.0);
    }

    private Post findMostPositivePost(List<Post> posts) {
        return posts.stream()
                .max(Comparator.comparingDouble(Post::getSentimentScore))
                .orElse(null);
    }

    private Post findMostNegativePost(List<Post> posts) {
        return posts.stream()
                .min(Comparator.comparingDouble(Post::getSentimentScore))
                .orElse(null);
    }

    private List<String> extractSubreddits(List<Post> posts) {
        return posts.stream()
                .map(Post::getSubreddit)
                .distinct()
                .collect(Collectors.toList());
    }

    private OffsetDateTime getEarliestPostDate(List<Post> posts) {
        return posts.stream()
                .map(Post::getCreationDate)
                .min(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());
    }

    private OffsetDateTime getLatestPostDate(List<Post> posts) {
        return posts.stream()
                .map(Post::getCreationDate)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());
    }

    private List<String> getMatchedKeywords(List<Post> posts) {
        return posts.stream()
                .flatMap(post -> post.getComments().stream())
                .flatMap(comment -> Arrays.stream(comment.getText().split(" ")))
                .filter(word -> word.length() > 3)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getMatchedAuthors(List<Post> posts) {
        return posts.stream()
                .map(Post::getAuthor)
                .distinct()
                .collect(Collectors.toList());
    }

    private Double calculateCorrelationSentimentUpvotes(List<Post> posts) {
        return calculateCorrelation(posts, Post::getSentimentScore, Post::getUpvotes);
    }

    private Double calculateCorrelationLengthSentiment(List<Post> posts) {
        return calculateCorrelation(posts, Post::getSentimentScore, post -> post.getTopComment().length());
    }

    private Double calculateCorrelation(List<Post> posts, SentimentExtractor sentimentExtractor, ValueExtractor valueExtractor) {
        double sentimentSum = 0;
        double valueSum = 0;
        double sentimentValueProductSum = 0;
        double sentimentSquaredSum = 0;
        double valueSquaredSum = 0;

        for (Post post : posts) {
            double sentiment = sentimentExtractor.extract(post);
            int value = valueExtractor.extract(post);

            sentimentSum += sentiment;
            valueSum += value;
            sentimentValueProductSum += sentiment * value;
            sentimentSquaredSum += sentiment * sentiment;
            valueSquaredSum += value * value;
        }

        int n = posts.size();
        double numerator = n * sentimentValueProductSum - sentimentSum * valueSum;
        double denominator = Math.sqrt((n * sentimentSquaredSum - sentimentSum * sentimentSum) *
                (n * valueSquaredSum - valueSum * valueSum));

        return (denominator == 0) ? 0.0 : numerator / denominator;
    }

    private List<String> topKeywordsBySentiment(List<Post> posts) {
        Map<String, Double> keywordSentimentMap = new HashMap<>();

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                double sentimentScore = comment.getSentimentScore();

                String[] words = comment.getText().split("\\s+");
                for (String word : words) {
                    if (!isStopWord(word)) {
                        keywordSentimentMap.merge(word.toLowerCase(), sentimentScore, Double::sum);
                    }
                }
            }
        }

        return keywordSentimentMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isStopWord(String word) {
        List<String> stopWords = Arrays.asList("the", "and", "is", "in", "to", "a", "of", "for", "on", "with", "as", "it", "at", "by");
        return stopWords.contains(word.toLowerCase());
    }

    private List<String> frequentTerms(List<Post> posts) {
        Map<String, Integer> termFrequencyMap = new HashMap<>();

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                String[] words = comment.getText().split("\\s+");
                for (String word : words) {
                    if (!isStopWord(word)) {
                        termFrequencyMap.merge(word.toLowerCase(), 1, Integer::sum);
                    }
                }
            }
        }

        return termFrequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> generateWarnings(List<Post> posts, boolean trained) {
        List<String> warnings = new ArrayList<>();
        if (posts.isEmpty()) warnings.add("No posts available for analysis");
        if (!trained) warnings.add("Model training failed or skipped");
        return warnings;
    }

    @FunctionalInterface
    private interface SentimentExtractor {
        double extract(Post post);
    }

    @FunctionalInterface
    private interface ValueExtractor {
        int extract(Post post);
    }
}