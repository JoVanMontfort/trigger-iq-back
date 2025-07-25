package damnosol.triggeriq.sentiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommentTextWekaTrainer {

    private static final Logger logger = LoggerFactory.getLogger(CommentTextWekaTrainer.class);

    private final int maxWords;
    private final boolean tfTransform;
    private final boolean idfTransform;

    public CommentTextWekaTrainer(WekaTrainerProperties config) {
        this.maxWords = config.getMaxWords();
        this.tfTransform = config.isTfTransform();
        this.idfTransform = config.isIdfTransform();
        logger.info("🔧 WekaTrainer config → maxWords={}, TF={}, IDF={}", maxWords, tfTransform, idfTransform);
    }

    public Instances convertTextToInstances(String[] commentTexts, double[] upvotes) throws Exception {
        // Step 1: Define attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("commentText", (List<String>) null)); // string attribute
        attributes.add(new Attribute("upvotes"));                          // numeric class attribute

        // Step 2: Create empty dataset
        Instances data = new Instances("CommentDataset", attributes, commentTexts.length);
        data.setClassIndex(1); // upvotes is the target

        // Step 3: Populate instances
        for (int i = 0; i < commentTexts.length; i++) {
            DenseInstance instance = new DenseInstance(2);
            instance.setValue(attributes.get(0), commentTexts[i]);
            instance.setValue(attributes.get(1), upvotes[i]);
            instance.setDataset(data); // ← This is CRITICAL for string attributes
            data.add(instance);
        }

        // Step 4: Configure TF-IDF filter
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first"); // only apply to the comment text
        filter.setLowerCaseTokens(true);
        filter.setTFTransform(tfTransform);
        filter.setIDFTransform(idfTransform);
        filter.setWordsToKeep(maxWords);
        filter.setOutputWordCounts(true);
        filter.setInputFormat(data); // ← Prepares the filter correctly

        // Step 5: Apply filter
        return Filter.useFilter(data, filter);
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