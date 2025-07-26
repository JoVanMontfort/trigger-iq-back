package damnosol.triggeriq.sentiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommentTextWekaTrainer {

    private static final Logger logger = LoggerFactory.getLogger(CommentTextWekaTrainer.class);

    private final int maxWords;
    private final boolean tfTransform;
    private final boolean idfTransform;
    private final boolean enableAttributeSelection = false; // toggle this if you want feature selection
    private final File cacheDir = new File("weka-cache");

    public CommentTextWekaTrainer(WekaTrainerProperties config) {
        this.maxWords = config.getMaxWords();
        this.tfTransform = config.isTfTransform();
        this.idfTransform = config.isIdfTransform();
        logger.info("🔧 WekaTrainer config → maxWords={}, TF={}, IDF={}", maxWords, tfTransform, idfTransform);
    }

    public Instances convertTextToInstances(String[] commentTexts, double[] upvotes) throws Exception {
        String cacheKey = generateCacheKey(commentTexts, upvotes);
        File cacheFile = new File(cacheDir, cacheKey + ".arff");

        // 🔁 Use cached file if available
        if (cacheFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                Instances cached = new Instances(reader);
                cached.setClassIndex(cached.numAttributes() - 1);
                return cached;
            }
        }

        // Step 1: Define attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("commentText", (List<String>) null)); // string attribute
        attributes.add(new Attribute("upvotes")); // numeric class attribute

        // Step 2: Create dataset
        Instances data = new Instances("CommentDataset", attributes, commentTexts.length);
        data.setClassIndex(1); // target = upvotes

        for (int i = 0; i < commentTexts.length; i++) {
            DenseInstance instance = new DenseInstance(2);
            instance.setValue(attributes.get(0), commentTexts[i]);
            instance.setValue(attributes.get(1), upvotes[i]);
            instance.setDataset(data);
            data.add(instance);
        }

        // Step 3: TF-IDF Filter
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first");
        filter.setLowerCaseTokens(true);
        filter.setTFTransform(tfTransform);
        filter.setIDFTransform(idfTransform);
        filter.setWordsToKeep(maxWords);
        filter.setStopwordsHandler(new Rainbow()); // 🛑 Remove stopwords
        filter.setOutputWordCounts(true);          // 🔢 Use word counts
        filter.setDoNotOperateOnPerClassBasis(true); // 🧪 Faster for regression
        filter.setInputFormat(data);

        Instances filtered = Filter.useFilter(data, filter);

        // Step 4: Attribute Selection (optional)
        if (enableAttributeSelection) {
            AttributeSelection attributeSelectionFilter = new AttributeSelection();
            InfoGainAttributeEval evaluator = new InfoGainAttributeEval();
            Ranker search = new Ranker();
            search.setNumToSelect(300); // top 300 features

            attributeSelectionFilter.setEvaluator(evaluator);
            attributeSelectionFilter.setSearch(search);
            attributeSelectionFilter.setInputFormat(filtered);

            filtered = Filter.useFilter(filtered, attributeSelectionFilter);
        }

        // Step 5: Cache result for reuse
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
            writer.write(filtered.toString());
        }

        return filtered;
    }

    private String generateCacheKey(String[] texts, double[] targets) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String text : texts) {
            digest.update(text.getBytes(StandardCharsets.UTF_8));
        }
        for (double d : targets) {
            digest.update(Double.toString(d).getBytes(StandardCharsets.UTF_8));
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String trainCommentTextModel(List<String> commentTexts, List<Double> upvotes, String modelType) {
        if ("LinearRegression".equalsIgnoreCase(modelType)) {
            return trainLinearRegression(commentTexts, upvotes); // Call private method for Linear Regression
        } else if ("RandomForest".equalsIgnoreCase(modelType)) {
            return trainRandomForest(commentTexts, upvotes); // Call private method for Random Forest
        } else {
            return "❌ Invalid model type provided. Use 'LinearRegression' or 'RandomForest'.";
        }
    }

    private String trainLinearRegression(List<String> commentTexts, List<Double> upvotes) {
        try {
            Instances tfidfData = buildTFIDFDataset(commentTexts, upvotes);
            LinearRegression model = new LinearRegression();
            model.buildClassifier(tfidfData);
            return model.toString();
        } catch (Exception e) {
            logger.error("❌ Weka LinearRegression training failed", e);
            return "Training failed: " + e.getMessage();
        }
    }

    private String trainRandomForest(List<String> commentTexts, List<Double> upvotes) {
        try {
            Instances tfidfData = buildTFIDFDataset(commentTexts, upvotes);
            RandomForest forest = new RandomForest();
            forest.setNumIterations(100);
            forest.setSeed(1);
            forest.buildClassifier(tfidfData);
            return forest.toString();
        } catch (Exception e) {
            logger.error("❌ Weka RandomForest training failed", e);
            return "Training failed: " + e.getMessage();
        }
    }

    private Instances buildTFIDFDataset(List<String> commentTexts, List<Double> upvotes) throws Exception {
        Instances rawData = buildRawDataset(commentTexts, upvotes);
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first");
        filter.setTFTransform(tfTransform);
        filter.setIDFTransform(idfTransform);
        filter.setLowerCaseTokens(true);
        filter.setOutputWordCounts(true);
        filter.setWordsToKeep(maxWords);

        filter.setInputFormat(rawData);
        return Filter.useFilter(rawData, filter);
    }

    private Instances buildRawDataset(List<String> commentTexts, List<Double> upvotes) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("commentText", (List<String>) null)); // string attribute
        attributes.add(new Attribute("upvotes"));                          // numeric class attribute

        Instances data = new Instances("CommentDataset", attributes, commentTexts.size());
        data.setClassIndex(1); // "upvotes" is the target

        for (int i = 0; i < commentTexts.size(); i++) {
            DenseInstance instance = new DenseInstance(2);
            instance.setValue(attributes.get(0), commentTexts.get(i));
            instance.setValue(attributes.get(1), upvotes.get(i));
            data.add(instance);
        }

        return data;
    }
}