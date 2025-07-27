package damnosol.triggeriq.sentiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import damnosol.triggeriq.sentiment.reddit.Comment;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.result.AnalysisResult;
import damnosol.triggeriq.sentiment.services.storage.MinioStorageService;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RedditPostsSentimentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RedditPostsSentimentAnalyzer.class);

    private final StanfordCoreNLP pipeline;
    private final ObjectMapper mapper;
    private final MinioStorageService minioStorageService;
    private final CommentTextWekaTrainer commentTextWekaTrainer;
    private final SentimentUpvoteAnalysis sentimentUpvoteAnalysis;
    private final MultivariateSentimentAnalysis multivariateSentimentAnalysis;

    public RedditPostsSentimentAnalyzer(ObjectMapper mapper,
                                        MinioStorageService minioStorageService,
                                        CommentTextWekaTrainer commentTextWekaTrainer,
                                        SentimentUpvoteAnalysis sentimentUpvoteAnalysis,
                                        MultivariateSentimentAnalysis multivariateSentimentAnalysis) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
        this.mapper = mapper;
        this.minioStorageService = minioStorageService;
        this.commentTextWekaTrainer = commentTextWekaTrainer;
        this.sentimentUpvoteAnalysis = sentimentUpvoteAnalysis;
        this.multivariateSentimentAnalysis = multivariateSentimentAnalysis;
    }

    public AnalysisResult analyze(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.warn("❌ No posts available for sentiment analysis.");
            return AnalysisResult.builder()
                    .posts(List.of())
                    .trained(false)
                    .modelUsed("N/A")
                    .rSquared(0.0)
                    .featureImportance(Map.of())
                    .predictionSamples(List.of("No data available."))
                    .build();
        }

        // Step 1: Preprocess posts (e.g., sentiment scoring)
        processPosts(posts);

        // Step 2: Correlation & analysis (optional, but might feed into model features)
        analyzeCorrelations(posts);
        performMultivariateAnalysis(posts);

        // Step 3: Prepare data for ML
        AlignedData data = extractAlignedCommentFeatures(posts);

        boolean trained = false;
        String modelUsed = "N/A";
        double rSquared = 0.0;
        Map<String, Double> featureImportance = Map.of();
        List<String> predictionSamples = List.of();

        if (validateAlignedData("AlignedFeatures", data)) {
            modelUsed = "Weka";
            trained = true;
            performWekaModelTraining(data);
            rSquared = calculateRSquared(data);
            featureImportance = extractFeatureImportance(data);
            predictionSamples = getPredictionSamples(posts);
        } else {
            logger.warn("⚠️ Skipping training — invalid aligned feature arrays.");
        }

        // Optional: train using comment text only
        trainOnCommentText(data.commentTexts(), data.upvotes());

        return AnalysisResult.builder()
                .posts(posts)
                .trained(trained)
                .modelUsed(modelUsed)
                .rSquared(rSquared)
                .featureImportance(featureImportance)
                .predictionSamples(predictionSamples)
                .build();
    }

    private double calculateRSquared(AlignedData data) {
        double[] actual = data.upvotes();
        double[] predicted = new double[actual.length];

        // Mock: Predict using average sentiment-based guess (replace with actual model output)
        double mean = Arrays.stream(actual).average().orElse(0.0);
        Arrays.fill(predicted, mean);

        double ssTotal = 0.0;
        double ssResidual = 0.0;
        for (int i = 0; i < actual.length; i++) {
            ssTotal += Math.pow(actual[i] - mean, 2);
            ssResidual += Math.pow(actual[i] - predicted[i], 2);
        }

        return ssTotal == 0 ? 0.0 : 1 - (ssResidual / ssTotal);
    }

    private Map<String, Double> extractFeatureImportance(AlignedData data) {
        // Mock: Use dummy importance values. Replace with actual Weka output.
        Map<String, Double> importance = new LinkedHashMap<>();
        importance.put("length", 0.5);
        importance.put("sentiment", 0.35);
        importance.put("containsQuestionMark", 0.15);
        return importance;
    }

    private Map<String, Double> extractFeatureImportanceFromLinearRegression(Instances trainingData) throws Exception {
        LinearRegression model = new LinearRegression();
        model.buildClassifier(trainingData);

        double[] coefficients = model.coefficients(); // includes intercept at end
        Map<String, Double> featureImportance = new LinkedHashMap<>();

        for (int i = 0; i < trainingData.numAttributes() - 1; i++) { // skip class
            String attrName = trainingData.attribute(i).name();
            featureImportance.put(attrName, coefficients[i]);
        }

        return featureImportance;
    }

    private Map<String, Double> extractFeatureImportanceWithInfoGain(Instances data) throws Exception {
        AttributeSelection selector = new AttributeSelection();
        InfoGainAttributeEval evaluator = new InfoGainAttributeEval();
        Ranker ranker = new Ranker();

        selector.setEvaluator(evaluator);
        selector.setSearch(ranker);
        selector.SelectAttributes(data);

        Map<String, Double> featureImportance = new LinkedHashMap<>();
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            String name = data.attribute(i).name();
            double score = evaluator.evaluateAttribute(i);
            featureImportance.put(name, score);
        }

        return featureImportance.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> b, LinkedHashMap::new));
    }

    private List<String> getPredictionSamples(List<Post> posts) {
        return posts.stream()
                .limit(3)
                .map(post -> {
                    String comment = post.getTopComment();
                    int length = comment != null ? comment.length() : 0;
                    double mockScore = length * 0.01; // Mock predicted score
                    return String.format("Comment: \"%s\" → Predicted Upvotes: %.2f",
                            comment != null ? comment : "[No comment]", mockScore);
                })
                .collect(Collectors.toList());
    }

    /**
     * Helper method to process each post (analyze comments and upload)
     */
    private void processPosts(List<Post> posts) {
        posts.forEach(post -> {
            analyzePostAndComments(post);
            uploadPostToMinio(post);
        });
    }

    /**
     * Helper method to perform sentiment-upvote and comment-sentiment-upvote correlations
     */
    private void analyzeCorrelations(List<Post> posts) {
        analyzeSentimentUpvoteCorrelation(posts);
        analyzeCommentSentimentUpvoteCorrelation(posts);
    }

    /**
     * Helper method for multivariate sentiment analysis
     */
    private void performMultivariateAnalysis(List<Post> posts) {
        multivariateSentimentAnalysisAnalysis(posts);
    }

    /**
     * Helper method to perform Weka model training (Linear Regression and Random Forest)
     */
    private void performWekaModelTraining(AlignedData data) {
        try {
            String wekaLinear = multivariateSentimentAnalysis.performWekaRegression(data.upvotes(), data.sentiments(), data.commentTexts());
            String wekaRF = multivariateSentimentAnalysis.performWekaRandomForestRegression(
                    data.sentiments(), data.lengths(), data.hasQuestions(), data.upvotes());

            logger.info("➤ Weka Linear Regression Model:\n{}", wekaLinear);
            logger.info("➤ Weka RandomForest Regression Model:\n{}", wekaRF);
        } catch (Exception e) {
            logger.error("❌ Error during Weka model training", e);
        }
    }

    private String analyze(String text) {
        if (text == null || text.trim().isEmpty()) return "Unknown";

        try {
            CoreDocument doc = new CoreDocument(text.trim());  // Trim any leading/trailing spaces
            pipeline.annotate(doc);

            // Handle multiple sentences; can either return aggregated sentiment or just the first
            return doc.sentences().stream()
                    .map(CoreSentence::sentiment)
                    .findFirst()
                    .orElse("Unknown");  // Fallback to "Unknown" if no sentiment found
        } catch (Exception e) {
            logger.error("Sentiment analysis failed", e);
            return "Unknown";
        }
    }

    /**
     * Method to train the model using comment text as a feature
     */
    private void trainOnCommentText(String[] commentTexts, double[] upvotes) {
        try {
            Instances data = commentTextWekaTrainer.convertTextToInstances(commentTexts, upvotes);
            // Assuming you are using a LinearRegression model for this part
            LinearRegression model = new LinearRegression();
            model.buildClassifier(data);

            logger.info("📘 Text-Based Linear Regression Model:\n{}", model.toString());
        } catch (Exception e) {
            logger.error("❌ Error training text-based Weka model: ", e);
        }
    }

    private boolean validateAlignedData(String featureName, AlignedData data) {
        boolean isValid = validateEqualLengths(featureName,
                data.sentiments(), data.lengths(), data.hasQuestions(), data.upvotes(), data.commentTexts());

        if (!isValid) {
            logger.warn("❌ Mismatched array lengths detected for {}.", featureName);
        }

        return isValid;
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

    private void analyzePostAndComments(Post post) {
        // Analyze post sentiment
        String postSentiment = analyze(post.getTitle());
        post.setSentiment(postSentiment);
        logSentiment(post.getTitle(), postSentiment, true);

        // Analyze each comment sentiment
        post.getComments().forEach(comment -> {
            String commentSentiment = analyze(comment.getText());
            comment.setSentiment(commentSentiment);
            logSentiment(comment.getText(), commentSentiment, false);
        });
    }

    private void analyzeSentimentUpvoteCorrelation(List<Post> posts) {
        List<Double> upvotes = new ArrayList<>();
        List<Double> sentiments = new ArrayList<>();

        for (Post post : posts) {
            double upvote = post.getUpvotes();
            double sentimentScore = convertSentimentToNumerical(post.getSentiment());

            if (isValidData(upvote, sentimentScore)) {
                upvotes.add(upvote);
                sentiments.add(sentimentScore);
            }
        }

        // Perform analysis only if enough data is available
        if (upvotes.size() < 2) {
            logger.warn("❌ Not enough data for correlation/causality analysis after filtering (only {} entries).", upvotes.size());
            return;
        }

        // Convert lists to arrays
        double[] upvoteArray = convertListToArray(upvotes);
        double[] sentimentArray = convertListToArray(sentiments);
        String[] commentTexts = extractCommentTexts(posts).toArray(new String[0]);

        logInputArraySizes(upvoteArray, sentimentArray, commentTexts);

        // Calculate correlation and causality
        double correlation = sentimentUpvoteAnalysis.calculatePearsonCorrelation(upvoteArray, sentimentArray);
        double[] causalityLinear = sentimentUpvoteAnalysis.performLinearRegression(upvoteArray, sentimentArray);

        String wekaModel = "⚠️ Skipped (array mismatch)";
        String wekaRFModel = "⚠️ Skipped (array mismatch)";

        if (validateEqualLengths("Weka Linear Regression", upvoteArray, sentimentArray, commentTexts)) {
            wekaModel = multivariateSentimentAnalysis.performWekaRegression(upvoteArray, sentimentArray, commentTexts);
        }

        // Check Weka RandomForest model
        double[] lengths = extractCommentLengths(posts);
        double[] hasQuestionMarks = extractHasQuestionMarks(posts);
        if (validateEqualLengths("Weka RandomForest", sentimentArray, lengths, hasQuestionMarks, upvoteArray)) {
            wekaRFModel = multivariateSentimentAnalysis.performWekaRandomForestRegression(sentimentArray, lengths, hasQuestionMarks, upvoteArray);
        }

        // Log results
        logAnalysisSummary(correlation, causalityLinear, wekaModel, wekaRFModel);
    }

    private void analyzeCommentSentimentUpvoteCorrelation(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.warn("❌ No posts provided for sentiment/upvote correlation.");
            return;
        }

        List<Double> commentUpvotes = new ArrayList<>();
        List<Double> commentSentiments = new ArrayList<>();

        for (Post post : posts) {
            if (post.getComments() == null) continue;

            for (Comment comment : post.getComments()) {
                if (comment == null || comment.getText() == null || comment.getText().isBlank()) {
                    logger.debug("⏭️ Skipping null/blank comment in post '{}'", post.getTitle());
                    continue;
                }

                double upvote = comment.getUpvotes();
                String sentiment = comment.getSentiment();
                if (sentiment == null) {
                    logger.debug("⏭️ Skipping comment with null sentiment in post '{}'", post.getTitle());
                    continue;
                }

                double sentimentScore = convertSentimentToNumerical(sentiment);
                if (isValidData(upvote, sentimentScore)) {
                    commentUpvotes.add(upvote);
                    commentSentiments.add(sentimentScore);

                    logger.info("✅ Including comment for analysis:");
                    logger.info("   ➤ Post Title: {}", post.getTitle());
                    logger.info("   ➤ Subreddit: {}", post.getSubreddit());
                    logger.info("   ➤ Comment Text: {}", comment.getText());
                    logger.info("   ➤ Upvotes: {}", upvote);
                    logger.info("   ➤ Sentiment (String): {}", sentiment);
                    logger.info("   ➤ Sentiment (Score): {}", sentimentScore);
                } else {
                    logger.debug("⏭️ Skipping comment with invalid data: upvotes = {}, sentimentScore = {}", upvote, sentimentScore);
                }
            }
        }

        if (commentUpvotes.size() < 3) {
            logger.warn("❌ Not enough valid comment data for correlation/causality analysis (need at least 3, found {}).", commentUpvotes.size());
            return;
        }

        double[] upvoteArray = convertListToArray(commentUpvotes);
        double[] sentimentArray = convertListToArray(commentSentiments);
        String[] commentTexts = extractCommentTexts(posts).stream()
                .filter(Objects::nonNull)
                .filter(t -> !t.isBlank())
                .toArray(String[]::new);

        logInputArraySizes(upvoteArray, sentimentArray, commentTexts);

        try {
            double correlation = sentimentUpvoteAnalysis.calculatePearsonCorrelation(upvoteArray, sentimentArray);
            double[] causalityLinear = sentimentUpvoteAnalysis.performLinearRegression(upvoteArray, sentimentArray);

            String wekaModel = "⚠️ Skipped (array mismatch)";
            String wekaRFModel = "⚠️ Skipped (array mismatch)";

            if (validateEqualLengths("Weka Linear Regression", upvoteArray, sentimentArray, commentTexts)) {
                wekaModel = multivariateSentimentAnalysis.performWekaRegression(upvoteArray, sentimentArray, commentTexts);
            }

            double[] lengths = extractCommentLengths(posts);
            double[] hasQuestionMarks = extractHasQuestionMarks(posts);

            if (validateEqualLengths("Weka RandomForest", sentimentArray, lengths, hasQuestionMarks, upvoteArray)) {
                wekaRFModel = multivariateSentimentAnalysis.performWekaRandomForestRegression(
                        sentimentArray, lengths, hasQuestionMarks, upvoteArray
                );
            }

            logAnalysisSummary(correlation, causalityLinear, wekaModel, wekaRFModel);

        } catch (SingularMatrixException e) {
            logger.error("❌ Singular matrix during regression: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error during correlation/causality analysis: {}", e.getMessage(), e);
        }
    }

    private void multivariateSentimentAnalysisAnalysis(List<Post> posts) {
        List<Double> upvotes = new ArrayList<>();
        List<Double> sentiments = new ArrayList<>();
        List<String> commentTexts = new ArrayList<>();

        for (Post post : posts) {
            post.getComments().forEach(comment -> {
                upvotes.add((double) comment.getUpvotes());
                sentiments.add(convertSentimentToNumerical(comment.getSentiment()));
                commentTexts.add(comment.getText());
            });
        }

        if (upvotes.size() < 2) {
            logger.warn("❌ Not enough comment data for multivariate analysis.");
            return;
        }

        // Convert lists to arrays
        double[] upvoteArray = convertListToArray(upvotes);
        double[] sentimentArray = convertListToArray(sentiments);
        String[] commentTextArray = commentTexts.toArray(new String[0]);

        // Perform multivariate sentiment analysis
        MultivariateSentimentAnalysis.RegressionResult result = multivariateSentimentAnalysis.analyze(upvoteArray, sentimentArray, commentTextArray);

        // Log results
        logger.info("Multivariate Sentiment Analysis Summary:");
        logger.info("-----------------------------------------");
        logger.info("➤ Regression Coefficients: {}", Arrays.toString(result.coefficients));
        logger.info("➤ R²: {}", result.rSquared);
    }

    private boolean isValidData(double upvote, double sentimentScore) {
        return upvote > 0 && sentimentScore != 0.0;
    }

    private double[] convertListToArray(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private void logInputArraySizes(double[] upvotes, double[] sentiment, String[] commentTexts) {
        logger.info("Input Array Sizes:");
        logger.info("➤ Upvotes: {}", upvotes.length);
        logger.info("➤ Sentiment: {}", sentiment.length);
        logger.info("➤ Comment Texts: {}", commentTexts.length);
    }

    private void logAnalysisSummary(double correlation, double[] causalityLinear, String wekaModel, String wekaRFModel) {
        logger.info("Sentiment-Upvote Analysis Summary:");
        logger.info("-----------------------------------");
        logger.info("➤ Pearson Correlation: {}", correlation);
        logger.info("➤ Linear Regression Coefficients: {}", Arrays.toString(causalityLinear));
        logger.info("➤ Weka Linear Regression Model:\n{}", wekaModel);
        logger.info("➤ Weka RandomForest Regression Model:\n{}", wekaRFModel);
    }

    private boolean validateEqualLengths(String label, Object... arrays) {
        if (arrays == null || arrays.length == 0) {
            logger.warn("⚠️ No arrays provided for length validation: {}", label);
            return false;
        }

        int expectedLength = -1;

        for (Object arr : arrays) {
            if (arr == null) {
                logger.warn("⚠️ Null array encountered in '{}'", label);
                return false;
            }

            int length = -1;
            if (arr instanceof double[]) length = ((double[]) arr).length;
            else if (arr instanceof int[]) length = ((int[]) arr).length;
            else if (arr instanceof float[]) length = ((float[]) arr).length;
            else if (arr instanceof String[]) length = ((String[]) arr).length;
            else if (arr instanceof Object[]) length = ((Object[]) arr).length;
            else {
                logger.warn("⚠️ Unsupported array type in '{}': {}", label, arr.getClass().getSimpleName());
                return false;
            }

            if (expectedLength == -1) {
                expectedLength = length;
            } else if (length != expectedLength) {
                logger.warn("⚠️ Length mismatch in '{}': Expected {}, but found {}", label, expectedLength, length);
                return false;
            }
        }

        return true;
    }

    private record AlignedData(
            double[] sentiments,
            double[] lengths,
            double[] hasQuestions,
            double[] upvotes,
            String[] commentTexts
    ) {
    }

    private AlignedData extractAlignedCommentFeatures(List<Post> posts) {
        List<Double> sentiments = new ArrayList<>();
        List<Double> lengths = new ArrayList<>();
        List<Double> hasQuestions = new ArrayList<>();
        List<Double> upvotes = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                double sentiment = convertSentimentToNumerical(comment.getSentiment());
                if (sentiment == 0.0 || comment.getUpvotes() <= 0) continue;

                String text = comment.getText();
                if (text == null || text.isBlank()) continue;

                sentiments.add(sentiment);
                lengths.add((double) text.length());
                hasQuestions.add(text.contains("?") ? 1.0 : 0.0);
                upvotes.add((double) comment.getUpvotes());
                texts.add(text);
            }
        }

        return new AlignedData(
                toDoubleArray(sentiments),
                toDoubleArray(lengths),
                toDoubleArray(hasQuestions),
                toDoubleArray(upvotes),
                texts.toArray(new String[0])
        );
    }

    private double[] toDoubleArray(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
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

    private double convertSentimentToNumericalExpanded(String sentiment) {
        if (sentiment == null) return 0.0;
        switch (sentiment.toLowerCase()) {
            case "very positive":
                return 1.0;
            case "positive":
                return 0.5;
            case "neutral":
                return 0.0;
            case "negative":
                return -0.5;
            case "very negative":
                return -1.0;
            default:
                return 0.0; // Default to neutral if unrecognized
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

    private List<String> extractCommentTexts(List<Post> posts) {
        List<String> commentTexts = new ArrayList<>();
        for (Post post : posts) {
            for (Comment comment : post.getComments()) {
                commentTexts.add(comment.getText());
            }
        }
        return commentTexts;
    }

    private double[] extractCommentLengths(List<Post> posts) {
        return posts.stream()
                .flatMap(post -> post.getComments().stream())
                .mapToDouble(comment -> comment.getText().length())
                .toArray();
    }

    private double[] extractHasQuestionMarks(List<Post> posts) {
        return posts.stream()
                .flatMap(post -> post.getComments().stream())
                .mapToDouble(comment -> comment.getText().contains("?") ? 1.0 : 0.0)
                .toArray();
    }
}