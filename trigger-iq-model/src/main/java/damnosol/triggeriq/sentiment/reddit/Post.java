package damnosol.triggeriq.sentiment.reddit;

import java.time.Instant;
import java.util.List;

public class Post {
    private String title;
    private String sentiment;
    private Instant date;
    private List<Comment> comments;

    // Constructor
    public Post(String title, String sentiment, Instant date, List<Comment> comments) {
        this.title = title;
        this.sentiment = sentiment;
        this.date = date;
        this.comments = comments;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "[" + sentiment + "] " + title + " (Posted on: " + date.toString() + ")";
    }
}