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
    private String subreddit; // New field for subreddit
    private String id; // New field for post ID

    public Post() {
    }

    // Constructor with @JsonCreator to assist Jackson in creating a Post from JSON
    @JsonCreator
    public Post(
            @JsonProperty("title") String title,
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("upvotes") int upvotes,
            @JsonProperty("date") OffsetDateTime date,
            @JsonProperty("comments") List<Comment> comments,
            @JsonProperty("subreddit") String subreddit, // New parameter for subreddit
            @JsonProperty("id") String id) { // New parameter for post ID
        this.title = title;
        this.sentiment = sentiment;
        this.upvotes = upvotes;
        this.date = date;
        this.comments = comments;
        this.subreddit = subreddit;
        this.id = id;
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

    @JsonProperty("subreddit")
    public String getSubreddit() {
        return subreddit;
    }

    @JsonProperty("subreddit")
    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "[" + sentiment + "] " + title + " (Posted on: " + date.toString() + ") from subreddit: " + subreddit;
    }
}