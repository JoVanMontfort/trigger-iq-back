package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class Comment implements Serializable {

    private String text;
    private String sentiment;
    private String author;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime date;

    public Comment() {
    }

    // Constructor with @JsonCreator to assist Jackson in creating a Comment from JSON
    @JsonCreator
    public Comment(
            @JsonProperty("text") String text,
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("author") String author,
            @JsonProperty("date") OffsetDateTime date) {
        this.text = text;
        this.sentiment = sentiment;
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

    @Override
    public String toString() {
        return "[" + sentiment + "] " + text + " (Author: " + author + ", Date: " + date + ")";
    }
}