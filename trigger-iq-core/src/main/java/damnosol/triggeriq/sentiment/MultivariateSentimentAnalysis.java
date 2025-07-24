package damnosol.triggeriq.sentiment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Component;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

@Component
@Slf4j
public class MultivariateSentimentAnalysis {

    public static class RegressionResult {
        public final double[] coefficients; // includes intercept
        public final double rSquared;

        public RegressionResult(double[] coefficients, double rSquared) {
            this.coefficients = coefficients;
            this.rSquared = rSquared;
        }

        @Override
        public String toString() {
            return "Coefficients (Intercept + Features): " + Arrays.toString(coefficients) +
                    "\nR²: " + rSquared;
        }
    }

    /**
     * Perform multivariate regression analysis using Apache Commons Math3.
     */
    public RegressionResult analyze(double[] upvotes, double[] sentiment, String[] commentTexts) {
        if (upvotes.length != sentiment.length || sentiment.length != commentTexts.length) {
            throw new IllegalArgumentException("Array lengths must match");
        }

        int n = upvotes.length;
        double[][] features = new double[n][3];

        // Construct feature matrix (sentiment, comment length, contains question mark)
        for (int i = 0; i < n; i++) {
            features[i][0] = sentiment[i];
            features[i][1] = commentTexts[i].length();
            features[i][2] = commentTexts[i].contains("?") ? 1.0 : 0.0;
        }

        // Perform regression using Apache Commons Math3
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(upvotes, features);
        double[] coefficients = regression.estimateRegressionParameters(); // [intercept, b1, b2, b3]

        // Predict upvotes using the model
        double[] predicted = predictValues(coefficients, features);

        // Compute R²
        double rSquared = computeRSquared(upvotes, predicted);

        return new RegressionResult(coefficients, rSquared);
    }

    /**
     * Perform linear regression using Weka.
     */
    public String performWekaRegression(double[] upvotes, double[] sentiment, String[] commentTexts) {
        if (upvotes.length != sentiment.length || sentiment.length != commentTexts.length) {
            return "Error: Array lengths must match.";
        }

        try {
            // Create features and instances for Weka
            Instances dataSet = createInstances(upvotes, sentiment, commentTexts);

            // Build the linear regression model using Weka
            LinearRegression model = new LinearRegression();
            model.buildClassifier(dataSet);

            // Predict the values using the Weka model
            double[] predicted = new double[upvotes.length];
            for (int i = 0; i < upvotes.length; i++) {
                DenseInstance instance = (DenseInstance) dataSet.instance(i);
                predicted[i] = model.classifyInstance(instance);
            }

            // Calculate R² manually
            double rSquared = computeRSquared(upvotes, predicted);

            // Return the Weka model and R² value
            return "__Weka Linear Regression Model__\n" + model.toString() + "\nR²: " + rSquared;
        } catch (Exception e) {
            // Handle exceptions and log error message
            log.error("Error performing Weka regression: {}", e.getMessage(), e);
            return "Error performing Weka regression: " + e.getMessage();
        }
    }

    /**
     * Perform randomforest using Weka.
     */
    public String performWekaRandomForestRegression(double[] sentimentScores,
                                                    double[] commentLengths,
                                                    double[] hasQuestionMarks,
                                                    double[] upvotes) {
        try {
            // Step 1: Define attributes
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("sentimentScore"));
            attributes.add(new Attribute("commentLength"));
            attributes.add(new Attribute("hasQuestionMark"));
            attributes.add(new Attribute("upvotes")); // Target variable

            // Step 2: Create dataset
            Instances dataset = new Instances("RedditCommentsRF", attributes, sentimentScores.length);
            dataset.setClassIndex(dataset.numAttributes() - 1); // upvotes = target

            for (int i = 0; i < sentimentScores.length; i++) {
                double[] values = new double[]{
                        sentimentScores[i],
                        commentLengths[i],
                        hasQuestionMarks[i],
                        upvotes[i]
                };
                dataset.add(new DenseInstance(1.0, values));
            }

            // Step 3: Build RandomForest
            RandomForest rf = new RandomForest();
            rf.setNumIterations(100);
            rf.buildClassifier(dataset);

            // Step 4: Evaluate model
            Evaluation eval = new Evaluation(dataset);
            eval.crossValidateModel(rf, dataset, 10, new Random(1));

            // Step 5: Feature importance using InfoGain
            AttributeSelection attrSel = new AttributeSelection();
            InfoGainAttributeEval infoGainEval = new InfoGainAttributeEval();
            Ranker ranker = new Ranker();

            attrSel.setEvaluator(infoGainEval);
            attrSel.setSearch(ranker);
            attrSel.SelectAttributes(dataset);

            double[][] rankedAttributes = attrSel.rankedAttributes();

            // Step 6: Build output string
            StringBuilder result = new StringBuilder();
            result.append("📊 RandomForest R²: ")
                    .append(String.format("%.4f", Math.pow(eval.correlationCoefficient(), 2)))
                    .append("\n\n");

            result.append("🌲 RandomForest Model:\n")
                    .append(rf.toString()).append("\n");

            result.append("🔥 Feature Importances (InfoGain):\n");
            for (double[] attr : rankedAttributes) {
                int index = (int) attr[0];
                double score = attr[1];
                result.append(String.format("➤ %s: %.5f\n", dataset.attribute(index).name(), score));
            }

            return result.toString();

        } catch (Exception e) {
            return "RandomForest training failed: " + e.getMessage();
        }
    }

    /**
     * Helper method to create Instances for Weka.
     */
    private Instances createInstances(double[] upvotes, double[] sentiment, String[] commentTexts) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("sentiment"));
        attributes.add(new Attribute("length"));
        attributes.add(new Attribute("hasQuestion"));
        attributes.add(new Attribute("upvotes"));

        // Create the Instances object and set upvotes as the target variable (class)
        Instances dataSet = new Instances("CommentFeatures", attributes, upvotes.length);
        dataSet.setClassIndex(3);

        // Populate Instances with feature data
        for (int i = 0; i < upvotes.length; i++) {
            double[] values = new double[4];
            values[0] = sentiment[i];
            values[1] = commentTexts[i].length();
            values[2] = commentTexts[i].contains("?") ? 1.0 : 0.0;
            values[3] = upvotes[i];
            dataSet.add(new DenseInstance(1.0, values));
        }

        return dataSet;
    }

    /**
     * Helper method to predict upvotes using the model coefficients and feature data.
     */
    private double[] predictValues(double[] coefficients, double[][] features) {
        int n = features.length;
        double[] predicted = new double[n];
        for (int i = 0; i < n; i++) {
            predicted[i] = coefficients[0]; // intercept
            for (int j = 0; j < features[i].length; j++) {
                predicted[i] += coefficients[j + 1] * features[i][j];
            }
        }
        return predicted;
    }

    /**
     * Helper method to compute the R² value for regression fit.
     */
    private double computeRSquared(double[] actual, double[] predicted) {
        double meanY = Arrays.stream(actual).average().orElse(0.0);
        double ssTotal = Arrays.stream(actual).map(y -> Math.pow(y - meanY, 2)).sum();
        double ssResidual = 0.0;

        for (int i = 0; i < actual.length; i++) {
            ssResidual += Math.pow(actual[i] - predicted[i], 2);
        }

        return 1 - (ssResidual / ssTotal);
    }
}