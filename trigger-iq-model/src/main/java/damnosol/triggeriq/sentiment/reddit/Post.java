package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class Post implements Serializable {

    private String title;
    private String sentiment;
    private int upvotes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime date;

    private List<Comment> comments;

    public Post() {
    }

    // Constructor with @JsonCreator to assist Jackson in creating a Post from JSON
    @JsonCreator
    public Post(
            @JsonProperty("title") String title,
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("upvotes") int upvotes,
            @JsonProperty("date") OffsetDateTime date,
            @JsonProperty("comments") List<Comment> comments) {
        this.title = title;
        this.sentiment = sentiment;
        this.upvotes = upvotes;
        this.date = date;
        this.comments = comments;
    }

    // Getters and setters
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
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

    @JsonProperty("date")
    public OffsetDateTime getDate() {
        return date;
    }

    @JsonProperty("date")
    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    @JsonProperty("comments")
    public List<Comment> getComments() {
        return comments;
    }

    @JsonProperty("comments")
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "[" + sentiment + "] " + title + " (Posted on: " + date.toString() + ")";
    }
}