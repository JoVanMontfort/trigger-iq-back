package damnosol.triggeriq.sentiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import damnosol.triggeriq.sentiment.reddit.Comment;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.storage.MinioStorageService;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class SentimentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalyzer.class);

    private final StanfordCoreNLP pipeline;
    private final ObjectMapper mapper;
    private final MinioStorageService minioStorageService;
    private final SentimentUpvoteAnalysis sentimentUpvoteAnalysis;
    private final MultivariateSentimentAnalysis multivariateSentimentAnalysis;  // Add instance

    public SentimentAnalyzer(ObjectMapper mapper,
                             MinioStorageService minioStorageService,
                             SentimentUpvoteAnalysis sentimentUpvoteAnalysis,
                             MultivariateSentimentAnalysis multivariateSentimentAnalysis) {  // Inject new dependency
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,sentiment");
        this.pipeline = new StanfordCoreNLP(props);
        this.mapper = mapper;
        this.minioStorageService = minioStorageService;
        this.sentimentUpvoteAnalysis = sentimentUpvoteAnalysis;
        this.multivariateSentimentAnalysis = multivariateSentimentAnalysis;  // Initialize
    }

    public String analyze(String text) {
        if (text == null || text.isBlank()) return "Unknown";
        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);
        return doc.sentences().stream()
                .map(CoreSentence::sentiment)
                .findFirst()
                .orElse("Unknown");
    }

    public void analyzeAndPrintAndStore(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.warn("No posts available for sentiment analysis.");
            return;
        }

        for (Post post : posts) {
            analyzePostAndComments(post);
            uploadPostToMinio(post);
        }

        analyzeSentimentUpvoteCorrelation(posts);
        analyzeCommentSentimentUpvoteCorrelation(posts);

        // Integrate multivariate analysis here
        multivariateSentimentAnalysisAnalysis(posts);
    }

    private void analyzePostAndComments(Post post) {
        String postSentiment = analyze(post.getTitle());
        post.setSentiment(postSentiment);
        logSentiment(post.getTitle(), postSentiment, true);

        for (Comment comment : post.getComments()) {
            String commentSentiment = analyze(comment.getText());
            comment.setSentiment(commentSentiment);
            logSentiment(comment.getText(), commentSentiment, false);
        }
    }

    private void uploadPostToMinio(Post post) {
        try {
            String json = mapper.writeValueAsString(post);
            String key = String.format("reddit/posts/%s/%s.json", extractSubredditFromPost(post), post.getId());
            minioStorageService.uploadJson(key, json);
        } catch (Exception ex) {
            logger.warn("Failed to upload post to MinIO: {}", ex.getMessage(), ex);
        }
    }

    private void analyzeSentimentUpvoteCorrelation(List<Post> posts) {
        List<Double> filteredUpvotes = new ArrayList<>();
        List<Double> filteredSentiment = new ArrayList<>();

        for (Post post : posts) {
            double upvote = post.getUpvotes();
            double sentimentScore = convertSentimentToNumerical(post.getSentiment());

            // Filter out non-significant entries
            if (upvote > 0 && sentimentScore != 0.0) {
                filteredUpvotes.add(upvote);
                filteredSentiment.add(sentimentScore);
            }
        }

        if (filteredUpvotes.size() < 2) {
            logger.warn("Not enough data for correlation/causality analysis after filtering.");
            return;
        }

        double[] upvotes = filteredUpvotes.stream().mapToDouble(Double::doubleValue).toArray();
        double[] sentiment = filteredSentiment.stream().mapToDouble(Double::doubleValue).toArray();

        // Perform all three statistical methods
        double correlation = sentimentUpvoteAnalysis.calculatePearsonCorrelation(upvotes, sentiment);
        double[] causalityLinear = sentimentUpvoteAnalysis.performLinearRegression(upvotes, sentiment);
        String wekaModel = sentimentUpvoteAnalysis.performWekaRegression(upvotes, sentiment);

        // Log results
        logger.info("Sentiment-Upvote Analysis Summary (Filtered):");
        logger.info("---------------------------------------------");
        logger.info("➤ Pearson Correlation: {}", correlation);
        logger.info("➤ Linear Regression Coefficients: {}", Arrays.toString(causalityLinear));
        logger.info("➤ Weka Linear Regression Model:\n{}", wekaModel);
    }

    private void analyzeCommentSentimentUpvoteCorrelation(List<Post> posts) {
        List<Double> commentUpvotes = new ArrayList<>();
        List<Double> commentSentiments = new ArrayList<>();

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                double upvote = comment.getUpvotes();
                double sentimentScore = convertSentimentToNumerical(comment.getSentiment());

                if (upvote > 0 && sentimentScore != 0.0) {
                    commentUpvotes.add(upvote);
                    commentSentiments.add(sentimentScore);
                }
            }
        }

        if (commentUpvotes.size() < 2) {
            logger.warn("Not enough comment data for correlation/causality analysis after filtering.");
            return;
        }

        double[] upvotes = commentUpvotes.stream().mapToDouble(Double::doubleValue).toArray();
        double[] sentiment = commentSentiments.stream().mapToDouble(Double::doubleValue).toArray();

        double correlation = sentimentUpvoteAnalysis.calculatePearsonCorrelation(upvotes, sentiment);
        double[] causalityLinear = sentimentUpvoteAnalysis.performLinearRegression(upvotes, sentiment);
        String wekaModel = sentimentUpvoteAnalysis.performWekaRegression(upvotes, sentiment);

        logger.info("Comment Sentiment-Upvote Analysis Summary (Filtered):");
        logger.info("-----------------------------------------------------");
        logger.info("➤ Pearson Correlation: {}", correlation);
        logger.info("➤ Linear Regression Coefficients: {}", Arrays.toString(causalityLinear));
        logger.info("➤ Weka Linear Regression Model:\n{}", wekaModel);
    }

    private void multivariateSentimentAnalysisAnalysis(List<Post> posts) {
        List<Double> upvotes = new ArrayList<>();
        List<Double> sentiments = new ArrayList<>();
        List<String> commentTexts = new ArrayList<>();

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                upvotes.add((double) comment.getUpvotes());
                sentiments.add(convertSentimentToNumerical(comment.getSentiment()));
                commentTexts.add(comment.getText());
            }
        }

        if (upvotes.size() < 2) {
            logger.warn("Not enough comment data for multivariate analysis.");
            return;
        }

        double[] upvoteArray = upvotes.stream().mapToDouble(Double::doubleValue).toArray();
        double[] sentimentArray = sentiments.stream().mapToDouble(Double::doubleValue).toArray();
        String[] commentTextArray = commentTexts.toArray(new String[0]);

        // Perform Multivariate Regression
        MultivariateSentimentAnalysis.RegressionResult result = multivariateSentimentAnalysis.analyze(upvoteArray, sentimentArray, commentTextArray);

        logger.info("Multivariate Sentiment Analysis Summary:");
        logger.info("-----------------------------------------");
        logger.info("➤ Regression Coefficients: {}", Arrays.toString(result.coefficients));
        logger.info("➤ R²: {}", result.rSquared);
    }

    private double convertSentimentToNumerical(String sentiment) {
        if (sentiment == null) return 0.0;
        switch (sentiment.toLowerCase()) {
            case "positive":
                return 1.0;
            case "negative":
                return -1.0;
            case "neutral":
                return 0.0;
            default:
                return 0.0;
        }
    }

    private void logSentiment(String text, String sentiment, boolean isPost) {
        String icon = getSentimentIcon(sentiment);
        Ansi.Color color = getSentimentColor(sentiment);
        String label = isPost ? "[Post]" : "\t[Comment]";

        logger.info(String.valueOf(Ansi.ansi().fg(color).a(label + " " + icon + " " + text).reset()));
    }

    private String getSentimentIcon(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "positive":
                return "👍";
            case "negative":
                return "👎";
            case "neutral":
                return "😐";
            default:
                return "❓";
        }
    }

    private Ansi.Color getSentimentColor(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "positive":
                return Ansi.Color.GREEN;
            case "negative":
                return Ansi.Color.RED;
            case "neutral":
                return Ansi.Color.YELLOW;
            default:
                return Ansi.Color.WHITE;
        }
    }

    private String extractSubredditFromPost(Post post) {
        return post.getSubreddit() != null ? post.getSubreddit() : "unknown";
    }
}