package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class Comment implements Serializable {

    private String text;
    private String sentiment;
    private String author;
    private int upvotes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime date;

    public Comment() {
    }

    // Constructor with @JsonCreator to assist Jackson in creating a Comment from JSON
    @JsonCreator
    public Comment(
            @JsonProperty("text") String text,
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("upvotes") int upvotes,
            @JsonProperty("author") String author,
            @JsonProperty("date") OffsetDateTime date) {  // Include sentimentScore in constructor
        this.text = text;
        this.sentiment = sentiment;
        this.upvotes = upvotes;
        this.author = author;
        this.date = date;
    }

    // Getters and setters
    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("sentiment")
    public String getSentiment() {
        return sentiment;
    }

    @JsonProperty("sentiment")
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    @JsonProperty("upvotes")
    public int getUpvotes() {
        return upvotes;
    }

    @JsonProperty("upvotes")
    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    @JsonProperty("author")
    public String getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonProperty("date")
    public OffsetDateTime getDate() {
        return date;
    }

    @JsonProperty("date")
    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    /**
     * Converts sentiment string to a numerical value.
     *
     * @return A numerical sentiment score.
     */
    @JsonIgnore
    public double getSentimentScore() {
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

    @Override
    public String toString() {
        return "[" + sentiment + "] " + text + " (Author: " + author + ", Date: " + date + ")";
    }
}