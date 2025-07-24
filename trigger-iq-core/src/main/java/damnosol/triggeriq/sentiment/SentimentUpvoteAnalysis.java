package damnosol.triggeriq.sentiment;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Component;
import weka.classifiers.functions.LinearRegression;
import weka.core.*;

@Component
public class SentimentUpvoteAnalysis {

    /**
     * Calculate Pearson correlation coefficient between upvotes and sentiment.
     */
    public double calculatePearsonCorrelation(double[] upvotes, double[] sentiment) {
        try {
            if (upvotes.length != sentiment.length) {
                throw new IllegalArgumentException("Upvotes and sentiment arrays must have the same length.");
            }
            PearsonsCorrelation correlation = new PearsonsCorrelation();
            return correlation.correlation(upvotes, sentiment);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error in Pearson correlation calculation: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error in Pearson correlation calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Perform linear regression (causality) between upvotes (dependent) and sentiment (independent).
     */
    public double[] performLinearRegression(double[] upvotes, double[] sentiment) {
        try {
            if (upvotes.length != sentiment.length) {
                throw new IllegalArgumentException("Upvotes and sentiment arrays must have the same length.");
            }

            // Create independent variable (sentiment) and dependent variable (upvotes)
            double[][] X = new double[sentiment.length][1];  // Features (sentiment values)
            for (int i = 0; i < sentiment.length; i++) {
                X[i][0] = sentiment[i];  // Sentiment as independent variable
            }

            // Dependent variable (upvotes)
            double[] Y = upvotes;  // Upvotes as dependent variable

            // Perform regression
            OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
            regression.newSampleData(Y, X);

            // Get regression coefficients
            return regression.estimateRegressionParameters();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error in linear regression due to mismatched data length: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error in linear regression calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Perform regression analysis using Weka to model the relationship between sentiment and upvotes.
     */
    public String performWekaRegression(double[] upvotes, double[] sentiment) {
        try {
            if (upvotes.length != sentiment.length) {
                throw new IllegalArgumentException("Upvotes and sentiment arrays must have the same length.");
            }

            // Create attributes for sentiment and upvotes
            Attribute sentimentAttribute = new Attribute("sentiment");
            Attribute upvotesAttribute = new Attribute("upvotes");

            // Create an empty list to store instances
            FastVector<Instance> instances = new FastVector<>();

            // Create instances and add to the list (sentiment = 1, 0, -1; upvotes = some value)
            for (int i = 0; i < sentiment.length; i++) {
                // Create an instance with the feature values (sentiment, upvotes)
                double[] values = new double[]{sentiment[i], upvotes[i]};  // Sentiment, Upvotes
                DenseInstance instance = new DenseInstance(1.0, values);
                instances.addElement(instance);
            }

            // Create dataset
            Instances dataset = new Instances("SentimentUpvotes", new FastVector<Attribute>() {{
                add(sentimentAttribute);
                add(upvotesAttribute);
            }}, 0);

            // Add all instances to the dataset
            for (Instance instance : instances) {
                dataset.add(instance);
            }

            // Set class (target) variable to upvotes
            dataset.setClass(upvotesAttribute);

            // Perform regression using Weka LinearRegression model
            LinearRegression wekaRegression = new LinearRegression();
            wekaRegression.buildClassifier(dataset);

            // Return the Weka regression model details
            return wekaRegression.toString();

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error in Weka regression due to mismatched data length: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error in Weka regression calculation: " + e.getMessage(), e);
        }
    }
}