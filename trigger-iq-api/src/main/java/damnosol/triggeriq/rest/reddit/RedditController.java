package damnosol.triggeriq.rest.reddit;

import damnosol.triggeriq.dto.SentimentAnalysisResult;
import damnosol.triggeriq.sentiment.reddit.Comment;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.reddit.RedditFetcher;
import damnosol.triggeriq.sentiment.reddit.SentimentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reddit")
public class RedditController {

    private static final Logger logger = LoggerFactory.getLogger(RedditController.class);

    private final RedditFetcher redditFetcher;
    private final SentimentAnalysisService sentimentService;

    @Value("${reddit.default.limit}")
    private int defaultLimit;

    @Value("${reddit.default.min-upvotes}")
    private int defaultMinUpvotes;

    @Value("${reddit.default.keywords}")
    private String defaultKeywords; // comma-separated

    @Value("${reddit.default.authors}")
    private String defaultAuthors; // comma-separated

    @Value("${reddit.default.date-from}")
    private String defaultDateFrom;

    @Value("${reddit.default.date-to}")
    private String defaultDateTo;

    public RedditController(RedditFetcher redditFetcher, SentimentAnalysisService sentimentService) {
        this.redditFetcher = redditFetcher;
        this.sentimentService = sentimentService;
    }

    @GetMapping("/analyze")
    public SentimentAnalysisResult fetchAndAnalyzePosts(
            @RequestParam String subreddit,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) Integer minUpvotes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) List<String> authors
    ) {
        List<Post> fetched = redditFetcher.fetchTopPostsFiltered(
                subreddit,
                limit != null ? limit : defaultLimit,
                keywords != null ? keywords : List.of(defaultKeywords.split(",")),
                minUpvotes != null ? minUpvotes : defaultMinUpvotes,
                dateFrom != null ? dateFrom : OffsetDateTime.parse(defaultDateFrom),
                dateTo != null ? dateTo : OffsetDateTime.parse(defaultDateTo),
                authors
        );

        logger.info("✅ Fetched {} post(s) from subreddit: r/{}", fetched.size(), subreddit);
        for (Post post : fetched) {
            logger.info("📌 [{}] \"{}\" | Upvotes: {} | Subreddit: {} | Date: {} | ID: {}",
                    post.getSentiment(),
                    post.getTitle(),
                    post.getUpvotes(),
                    post.getSubreddit(),
                    post.getDate(),
                    post.getId());

            for (Comment comment : post.getComments()) {
                String text = comment.getText();
                if (text != null && text.length() > 100) {
                    text = text.substring(0, 100) + "...";
                }

                logger.info("    🗨️ Comment by {} | Sentiment: {} | Upvotes: {} | Text: {}",
                        comment.getAuthor(),
                        comment.getSentiment(),
                        comment.getUpvotes(),
                        text != null ? text : "(no text)");
            }

            logger.info("--------------------------------------------------");
        }

        return sentimentService.analyze(fetched); // Apply sentiment analysis after filtering
    }

}