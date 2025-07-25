
# 📊 TriggerIQ Sentiment Analysis: Statistics & Machine Learning Explained

This document explains the main statistical and machine learning components used in the sentiment analysis pipeline for `TriggerIQ`.

---

## 1. ✅ Sentiment-Upvote Correlation

We calculate the **Pearson correlation coefficient** between sentiment scores and upvote counts for:

- **Posts** (`analyzeSentimentUpvoteCorrelation`)
- **Comments** (`analyzeCommentSentimentUpvoteCorrelation`)

This helps identify:
- Whether positive or negative sentiment influences popularity
- How strongly emotion correlates with community approval

---

## 2. 🧠 Multivariate Correlation Analysis

We evaluate how multiple comment features interact to influence upvotes:
- Sentiment score (float)
- Comment length (int)
- Presence of question mark (boolean)

Techniques used:
- `Pearson correlation` matrix
- Feature alignment checks (to ensure consistent vector lengths)
- Informal feature importance ranking before model training

---

## 3. 🧪 Weka ML Models

### a. Linear Regression

We use Weka's `LinearRegression` to predict upvotes based on:
- Sentiment
- Length
- Question presence

It generates an equation of the form:
```
upvotes ≈ (a × sentiment) + (b × length) + (c × question) + d
```

### b. Random Forest Regression

A `RandomForest` is trained using the same features for non-linear modeling. This improves predictive power when features interact in complex ways.

---

## 4. 💬 Text-Based Model: Comment Text to Upvotes

We transform raw comment text into numeric features using:

- `StringToWordVector` (TF/IDF)
- Upvote count as the target

Two models are trained:
- `LinearRegression`: to learn from weighted keywords
- `RandomForest`: to capture deeper patterns across textual variation

This answers:
- How does raw language in comments influence upvotes?

---

## 5. ⚠️ Feature Validation

Before training, we validate that:
- All arrays (`sentiments`, `lengths`, `questionMarks`, `upvotes`, `commentTexts`) have the same length
- No `null` or `NaN` entries exist

---

## 6. 🪣 Storage

All original and processed post data is stored to MinIO for persistence and later dashboard analytics.

---

## Summary Table

| Analysis Type | Features Used | Output |
|---------------|---------------|--------|
| Sentiment vs Upvotes | sentiment, upvotes | Pearson correlation |
| Multivariate | sentiment, length, question | Feature interaction matrix |
| Weka Linear | sentiment, length, question | Predictive regression |
| Weka Random Forest | same | Non-linear model |
| Text → Upvotes | comment text (TF/IDF), upvotes | Trained model weights |

---

## Next Steps

- Add A/B testing for which comment tones lead to more traction
- Evaluate model performance with metrics (RMSE, R²)
- Visualize key word importance via TF-IDF scores
