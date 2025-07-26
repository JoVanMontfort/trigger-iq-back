package damnosol.triggeriq.rest.reddit;

import damnosol.triggeriq.dto.SentimentAnalysisResult;
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
                authors != null ? authors : List.of(defaultAuthors.split(","))
        );

        return sentimentService.analyze(fetched); // Apply sentiment analysis after filtering
    }

//    @GetMapping("/analyze")
//    public SentimentAnalysisResult fetchAndAnalyzePosts(
//            @RequestParam String subreddit,
//            @RequestParam(required = false) Integer limit,
//            @RequestParam(required = false) List<String> keywords,
//            @RequestParam(required = false) Integer minUpvotes,
//            @RequestParam(required = false) String dateFrom,
//            @RequestParam(required = false) String dateTo,
//            @RequestParam(required = false) List<String> authors
//    ) {
//        // Convert String to OffsetDateTime manually
//        OffsetDateTime parsedDateFrom = (dateFrom != null) ? OffsetDateTime.parse(dateFrom, DateTimeFormatter.ISO_OFFSET_DATE_TIME) : OffsetDateTime.parse(defaultDateFrom);
//        OffsetDateTime parsedDateTo = (dateTo != null) ? OffsetDateTime.parse(dateTo, DateTimeFormatter.ISO_OFFSET_DATE_TIME) : OffsetDateTime.parse(defaultDateTo);
//
//        logger.debug("Date From: {}", parsedDateFrom);
//        logger.debug("Date To: {}", parsedDateTo);
//
//        // Proceed with processing the request
//        List<Post> fetched = redditFetcher.fetchTopPostsFiltered(
//                subreddit,
//                limit != null ? limit : defaultLimit,
//                keywords != null ? keywords : List.of(defaultKeywords.split(",")),
//                minUpvotes != null ? minUpvotes : defaultMinUpvotes,
//                parsedDateFrom,
//                parsedDateTo,
//                authors != null ? authors : List.of(defaultAuthors.split(","))
//        );
//
//        return sentimentService.analyze(fetched); // Apply sentiment analysis after filtering
//    }

}