package damnosol.triggeriq.sentiment.reddit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RedditFetcher {

    private static final Logger logger = LoggerFactory.getLogger(RedditFetcher.class);

    private final RedditAuthService authService;
    private final RedditCacheDelegate cacheDelegate;

    public RedditFetcher(RedditAuthService authService, RedditCacheDelegate cacheDelegate) {
        this.authService = authService;
        this.cacheDelegate = cacheDelegate;
    }

    /**
     * Fetch and filter posts from a subreddit.
     *
     * @param subreddit  Subreddit name
     * @param limit      Number of top posts to fetch
     * @param keywords   Optional list of keywords to match
     * @param minUpvotes Optional minimum upvotes filter
     * @param dateFrom   Optional start of date range
     * @param dateTo     Optional end of date range
     * @param authors    Optional list of usernames to match in posts or comments
     */
    public List<Post> fetchTopPostsFiltered(
            String subreddit,
            int limit,
            List<String> keywords,
            Integer minUpvotes,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            List<String> authors
    ) {
        List<Post> rawPosts = fetchTopPosts(subreddit, limit);
        return filterPosts(rawPosts, keywords, minUpvotes, dateFrom, dateTo, authors);
    }

    public List<Post> fetchTopPosts(String subreddit, int limit) {
        String accessToken = authService.getAccessToken();
        String url = String.format("https://oauth.reddit.com/r/%s/hot.json?limit=%d", subreddit, limit);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<Post> posts = cacheDelegate.fetchTopPosts(url, entity);

        for (Post post : posts) {
            String permalink = "/r/" + subreddit + "/comments/" + post.getId();
            List<Comment> comments = fetchComments(permalink);
            post.setComments(comments);

            logger.info("📌 Post: [{}] \"{}\" | Upvotes: {} | Subreddit: {} | Date: {} | ID: {}",
                    post.getSentiment(),
                    post.getTitle(),
                    post.getUpvotes(),
                    post.getSubreddit(),
                    post.getDate(),
                    post.getId());

            for (Comment comment : comments) {
                String preview = comment.getText();
                if (preview != null && preview.length() > 100) {
                    preview = preview.substring(0, 100) + "...";
                }

                logger.info("    🗨️ Comment by {} | Sentiment: {} | Upvotes: {} | Text: {}",
                        comment.getAuthor(),
                        comment.getSentiment(),
                        comment.getUpvotes(),
                        preview != null ? preview : "(no text)");
            }

            logger.info("--------------------------------------------------");
        }

        return posts;
    }

    public List<Comment> fetchComments(String postUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "java:triggeriq.reddit:v1.0 (by /u/No-Economics9519)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return cacheDelegate.fetchComments(postUrl, entity);
    }

    private List<Post> filterPosts(
            List<Post> posts,
            List<String> keywords,
            Integer minUpvotes,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            List<String> authors
    ) {
        Pattern keywordPattern = (keywords != null && !keywords.isEmpty()) ? buildKeywordPattern(keywords) : null;
        Set<String> authorSet = (authors != null) ? new HashSet<>(authors.stream().map(String::toLowerCase).toList()) : null;

        logger.info("🔍 Starting post filtering:");
        logger.info("   ➤ Keywords: {}", keywords != null ? keywords : "None");
        logger.info("   ➤ Min upvotes: {}", minUpvotes != null ? minUpvotes : "None");
        logger.info("   ➤ Date range: {} to {}",
                dateFrom != null ? dateFrom : "Any",
                dateTo != null ? dateTo : "Any");
        logger.info("   ➤ Authors: {}", authors != null ? authors : "None");

        int totalBefore = posts.size();
        List<Post> filtered = posts.stream()
                .map(post -> {
                    if (!passesPostFilters(post, minUpvotes, dateFrom, dateTo)) {
                        logger.debug("❌ Post '{}' filtered out by post-level criteria.", post.getTitle());
                        return null;
                    }

                    List<Comment> matchingComments = post.getComments().stream()
                            .filter(comment -> passesCommentFilters(comment, keywordPattern, authorSet))
                            .collect(Collectors.toList());

                    if (matchingComments.isEmpty() && keywordPattern != null && !matches(post.getTitle(), keywordPattern)) {
                        logger.debug("❌ Post '{}' filtered out (no matching comments or keyword in title).", post.getTitle());
                        return null;
                    }

                    return new Post(
                            post.getTitle(),
                            post.getSentiment(),
                            post.getUpvotes(),
                            post.getDate(),
                            matchingComments,
                            post.getSubreddit(),
                            post.getId()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        logger.info("✅ Filtered posts: {}/{}", filtered.size(), totalBefore);

        return filtered;
    }

    private boolean passesPostFilters(
            Post post,
            Integer minUpvotes,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (minUpvotes != null && post.getUpvotes() < minUpvotes) return false;
        if (from != null && post.getDate().isBefore(from)) return false;
        return to == null || !post.getDate().isAfter(to);
    }

    private boolean passesCommentFilters(Comment comment, Pattern pattern, Set<String> authors) {
        boolean matchesKeyword = pattern == null || matches(comment.getText(), pattern);
        boolean matchesAuthor = authors == null || authors.contains(Optional.ofNullable(comment.getAuthor()).orElse("").toLowerCase());
        return matchesKeyword && matchesAuthor;
    }

    private Pattern buildKeywordPattern(List<String> keywords) {
        String regex = keywords.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|", "\\b(", ")\\b"));
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private boolean matches(String text, Pattern pattern) {
        return text != null && !text.isBlank() && pattern.matcher(text).find();
    }
}