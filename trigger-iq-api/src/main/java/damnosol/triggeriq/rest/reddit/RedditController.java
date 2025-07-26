package damnosol.triggeriq.rest.reddit;

import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.reddit.RedditFetcher;
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

    private final RedditFetcher redditFetcher;

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

    public RedditController(RedditFetcher redditFetcher) {
        this.redditFetcher = redditFetcher;
    }

    @GetMapping("/posts")
    public List<Post> fetchFilteredPosts(
            @RequestParam String subreddit,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) Integer minUpvotes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) List<String> authors
    ) {
        return redditFetcher.fetchTopPostsFiltered(
                subreddit,
                limit != null ? limit : defaultLimit,
                keywords != null ? keywords : List.of(defaultKeywords.split(",")),
                minUpvotes != null ? minUpvotes : defaultMinUpvotes,
                dateFrom != null ? dateFrom : OffsetDateTime.parse(defaultDateFrom),
                dateTo != null ? dateTo : OffsetDateTime.parse(defaultDateTo),
                authors != null ? authors : List.of(defaultAuthors.split(","))
        );
    }
}