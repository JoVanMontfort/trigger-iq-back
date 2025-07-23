package damnosol.triggeriq.sentiment.reddit;

public class Comment {
    private String text;
    private String sentiment;
    private String author;
    private String date;

    // Constructor
    public Comment(String text, String sentiment, String author, String date) {
        this.text = text;
        this.sentiment = sentiment;
        this.author = author;
        this.date = date;
    }

    // Getters and setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "[" + sentiment + "] " + text + " (Author: " + author + ", Date: " + date + ")";
    }
}