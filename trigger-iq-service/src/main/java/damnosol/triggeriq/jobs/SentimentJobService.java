package damnosol.triggeriq.jobs;

import damnosol.triggeriq.sentiment.async.model.requests.SentimentJobRequest;
import damnosol.triggeriq.sentiment.async.model.responses.SentimentJobResult;
import damnosol.triggeriq.sentiment.async.model.status.JobStatus;
import damnosol.triggeriq.sentiment.async.redis.repositories.RedisSentimentJobRepository;
import damnosol.triggeriq.sentiment.dto.SentimentAnalysisResult;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.sentiment.reddit.RedditFetcher;
import damnosol.triggeriq.sentiment.reddit.SentimentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class SentimentJobService {

    private static final Logger logger = LoggerFactory.getLogger(SentimentJobService.class);

    private final RedisSentimentJobRepository repository;
    private final RedditFetcher redditFetcher;
    private final SentimentAnalysisService sentimentService;
    private final Executor asyncExecutor;

    public SentimentJobService(RedisSentimentJobRepository repository,
                               RedditFetcher redditFetcher,
                               SentimentAnalysisService sentimentService) {
        this.repository = repository;
        this.redditFetcher = redditFetcher;
        this.sentimentService = sentimentService;
        this.asyncExecutor = Executors.newCachedThreadPool(); // Or use Spring @Async
    }

    public String submitJob(SentimentJobRequest request) {
        String jobId = UUID.randomUUID().toString();
        SentimentJobResult init = new SentimentJobResult(JobStatus.IN_PROGRESS, null, null, OffsetDateTime.now());
        repository.save(jobId, init, Duration.ofHours(8));

        asyncExecutor.execute(() -> {
            try {
                List<Post> posts = redditFetcher.fetchTopPostsFiltered(
                        request.getSubreddit(),
                        Optional.ofNullable(request.getLimit()).orElse(25),
                        request.getKeywords(),
                        request.getMinUpvotes(),
                        request.getDateFrom(),
                        request.getDateTo(),
                        request.getAuthors()
                );
                SentimentAnalysisResult result = sentimentService.analyze(posts);
                repository.save(jobId, new SentimentJobResult(JobStatus.COMPLETED, result, null, OffsetDateTime.now()), Duration.ofHours(8));
            } catch (Exception e) {
                logger.error("""
                                ❌ Sentiment analysis job FAILED
                                ─────────────────────────────────
                                🆔 Job ID       : {}
                                📅 Date        : {}
                                📌 Subreddit   : {}
                                🔍 Keywords    : {}
                                👍 Min upvotes : {}
                                📆 Date range  : {} → {}
                                👤 Authors     : {}
                                🧨 Error       : {}
                                """,
                        jobId,
                        OffsetDateTime.now(),
                        request.getSubreddit(),
                        request.getKeywords() != null ? request.getKeywords() : "None",
                        request.getMinUpvotes() != null ? request.getMinUpvotes() : "None",
                        request.getDateFrom() != null ? request.getDateFrom() : "Any",
                        request.getDateTo() != null ? request.getDateTo() : "Any",
                        request.getAuthors() != null ? request.getAuthors() : "None",
                        e.getMessage(),
                        e
                );
                repository.save(jobId, new SentimentJobResult(JobStatus.FAILED, null, e.getMessage(), OffsetDateTime.now()), Duration.ofHours(8));
            }
        });

        return jobId;
    }

    public SentimentJobResult getJobStatus(String jobId) {
        return repository.findById(jobId);
    }
}