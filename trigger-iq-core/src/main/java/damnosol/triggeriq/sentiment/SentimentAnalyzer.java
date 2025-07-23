package damnosol.triggeriq.sentiment;

import damnosol.triggeriq.sentiment.reddit.Comment;
import damnosol.triggeriq.sentiment.reddit.Post;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

@Component
public class SentimentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalyzer.class);

    private final StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }

    public String analyze(String text) {
        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);

        return doc.sentences().stream()
                .map(CoreSentence::sentiment)
                .findFirst()
                .orElse("Unknown");
    }

    public void analyzeAndPrint(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.warn("No posts available for sentiment analysis.");
            return;
        }

        for (Post post : posts) {
            String postSentiment = analyze(post.getTitle());
            post.setSentiment(postSentiment);

            // Print post title with sentiment icon and color
            String postIcon = getSentimentIcon(postSentiment);
            logger.info(String.valueOf(Ansi.ansi().fg(getSentimentColor(postSentiment))
                    .a("\n[Post] " + postIcon + " " + post.getTitle()).reset()));

            for (Comment comment : post.getComments()) {
                String commentSentiment = analyze(comment.getText());
                comment.setSentiment(commentSentiment);
                String commentIcon = getSentimentIcon(commentSentiment);
                logger.info(String.valueOf(Ansi.ansi().fg(getSentimentColor(commentSentiment))
                        .a("\t[Comment] " + commentIcon + " " + comment.getText()).reset()));
            }
        }
    }

    private String getSentimentIcon(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "positive":
                return "👍"; // Thumbs up for positive sentiment
            case "negative":
                return "👎"; // Thumbs down for negative sentiment
            case "neutral":
                return "😐"; // Neutral face for neutral sentiment
            default:
                return "❓"; // Question mark for undefined sentiment
        }
    }

    private Ansi.Color getSentimentColor(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "positive":
                return Ansi.Color.GREEN; // Green for positive sentiment
            case "negative":
                return Ansi.Color.RED; // Red for negative sentiment
            case "neutral":
                return Ansi.Color.YELLOW; // Yellow for neutral sentiment
            default:
                return Ansi.Color.DEFAULT; // Default color for undefined sentiment
        }
    }
}