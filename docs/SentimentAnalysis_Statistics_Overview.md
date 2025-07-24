
# Statistical Methods and Metrics Used in Sentiment Analysis

This document provides an overview of the statistical methods and metrics used for sentiment analysis in the context of regression models, particularly focusing on RandomForest, Pearson Correlation, and Linear Regression.

## 1. **Pearson Correlation**
The Pearson Correlation is a measure of the **linear relationship** between two variables. It ranges from -1 to 1:
- **+1** indicates a perfect positive correlation.
- **-1** indicates a perfect negative correlation.
- **0** indicates no linear correlation.

In the context of sentiment analysis, the Pearson Correlation between sentiment scores and upvotes helps understand how strongly sentiment scores are related to the number of upvotes.

Formula for Pearson Correlation (r):
```
r = Σ((Xi - X̄)(Yi - Ȳ)) / √Σ(Xi - X̄)² * Σ(Yi - Ȳ)²
```

Where:
- Xi, Yi are the individual sample points of variables X and Y.
- X̄, Ȳ are the means of X and Y respectively.

## 2. **Linear Regression**
Linear Regression aims to model the relationship between two variables by fitting a linear equation to the observed data. In sentiment analysis, linear regression models how sentiment scores, comment lengths, or the presence of question marks predict upvotes.

The general form of the linear regression equation is:
```
Y = β0 + β1*X1 + β2*X2 + ... + βn*Xn
```
Where:
- **Y** is the dependent variable (e.g., upvotes).
- **X1, X2, ..., Xn** are independent variables (e.g., sentiment score, comment length).
- **β0, β1, ..., βn** are the coefficients of the regression.

### Coefficients Interpretation:
- **β0** is the intercept or the predicted value when all independent variables are 0.
- **β1, β2, ..., βn** are the coefficients that represent how much each independent variable contributes to the dependent variable.

## 3. **Random Forest**
Random Forest is an **ensemble learning** method that combines multiple decision trees to improve prediction accuracy and prevent overfitting. It works by constructing several trees during training and outputting the mean prediction for regression tasks.

### Key Metrics:
- **Number of Trees:** The number of decision trees used in the random forest model.
- **Out-of-Bag Error:** Error estimation performed by testing each sample using trees that did not see that sample during training.
- **Feature Importance:** It measures how useful each feature is for making predictions. Weka's `RandomForest` algorithm ranks features by calculating how much a feature contributes to reducing the prediction error.

Formula for Feature Importance:
- Based on how much each feature reduces the **impurity** (such as **Gini impurity** or **entropy**) in the decision trees.

## 4. **R-squared (R²)**
R-squared is a statistical measure that explains how well the regression model fits the data. It is the proportion of the variance in the dependent variable that is predictable from the independent variables.

R² value ranges from 0 to 1:
- **1** indicates that the regression model perfectly predicts the outcome.
- **0** indicates that the model does not explain any of the variability in the data.

Formula for R²:
```
R² = 1 - (Σ(Yi - Ŷi)² / Σ(Yi - Ȳ)²)
```
Where:
- **Yi** is the actual value of the dependent variable.
- **Ŷi** is the predicted value.
- **Ȳ** is the mean of the actual values.

## 5. **InfoGain (Information Gain) for Feature Selection**
InfoGain is a metric used to measure the importance of an attribute in decision trees. It quantifies the amount of information gained by knowing the value of a particular feature.

InfoGain is based on **entropy**, which measures the uncertainty of a dataset. It helps identify which features reduce uncertainty and improve predictions.

### InfoGain Formula:
```
InfoGain(S, A) = Entropy(S) - Σ ( |Sv| / |S| ) * Entropy(Sv)
```
Where:
- **S** is the dataset.
- **A** is the attribute.
- **Sv** is the subset of instances for which attribute **A** has a specific value.

## Conclusion
These statistical techniques, including Pearson Correlation, Linear Regression, Random Forest, R-squared, and InfoGain, are powerful tools for analyzing and understanding relationships between features in sentiment analysis tasks. By applying these methods, you can extract valuable insights into how sentiment correlates with upvotes, how different features affect the target variable, and how well your models are performing.

These metrics are essential for evaluating the **effectiveness** of the models and understanding how different features contribute to the outcomes.
