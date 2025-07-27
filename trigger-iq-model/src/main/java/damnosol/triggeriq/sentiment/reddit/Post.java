package damnosol.triggeriq.sentiment.reddit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class Post implements Serializable {

    private String title;
    private String sentiment;
    private int upvotes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime date;

    private List<Comment> comments;
    private String subreddit;
    private String id;

    public Post() {
    }

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

    /**
     * Get sentiment score for the post. Converts sentiment string to a numerical value.
     *
     * @return A numerical sentiment score.
     */
    @JsonIgnore
    public double getSentimentScore() {
        switch (sentiment.toLowerCase()) {
            case "positive":
                return 1.0;
            case "neutral":
                return 0.0;
            case "negative":
                return -1.0;
            default:
                return 0.0;  // Default to neutral if sentiment is unknown
        }
    }

    /**
     * Get the creation date of the post.
     *
     * @return The date the post was created.
     */
    @JsonIgnore
    public OffsetDateTime getCreationDate() {
        return this.date;
    }

    /**
     * Get the author of the post. Assumes that the author is the most upvoted comment's author.
     *
     * @return The author of the post.
     */
    @JsonIgnore
    public String getAuthor() {
        if (comments != null && !comments.isEmpty()) {
            // Return the author of the comment with the highest upvotes
            Optional<Comment> topComment = comments.stream()
                    .filter(c -> c.getText() != null && !c.getText().isBlank())
                    .max((a, b) -> Integer.compare(a.getUpvotes(), b.getUpvotes()));

            return topComment.map(Comment::getAuthor).orElse(null);
        }
        return null;  // No comments, no author
    }

    /**
     * Get the top comment for the post, based on most upvotes.
     *
     * @return The text of the top comment.
     */
    @JsonIgnore
    public String getTopComment() {
        if (comments == null || comments.isEmpty()) return null;

        return comments.stream()
                .filter(c -> c.getText() != null && !c.getText().isBlank())
                .max((a, b) -> Integer.compare(a.getUpvotes(), b.getUpvotes()))
                .map(Comment::getText)
                .orElse(null);
    }

    @Override
    public String toString() {
        return "[" + sentiment + "] " + title + " (Posted on: " + date.toString() + ") from subreddit: " + subreddit;
    }
}