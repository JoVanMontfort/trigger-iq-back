package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoraPipelineWorkers {

    static final String KEY_PENDING_URLS = "quora:pending";
    static final String KEY_TO_EXTRACT = "quora:to_extract";
    static final String KEY_SENTIMENT_PENDING = "sentiment:pending";

    private final StringRedisTemplate redisTemplate;
    private final QuoraArchiveFetcher archiveFetcher;
    private final QuoraAnswerExtractor extractor;
    private final QuoraRetryScheduler retryScheduler; // centralized backoff + DLQ

    // === Production entrypoints (infinite, async) ===
    @Async("quoraExecutor")
    public void archiveWorker() {
        runArchiveWorker(Integer.MAX_VALUE);
    }

    @Async("quoraExecutor")
    public void extractWorker() {
        runExtractWorker(Integer.MAX_VALUE);
    }

    // === Testable entrypoints (bounded loops) ===
    public void runArchiveWorker(int maxIterations) {
        int iterations = 0;
        try {
            while (!Thread.currentThread().isInterrupted() && iterations++ < maxIterations) {
                // short blocking timeout keeps prod efficient and tests snappy
                String url = redisTemplate.opsForList()
                        .rightPop(KEY_PENDING_URLS, 1, TimeUnit.SECONDS);

                if (url == null) continue;

                log.info("Archiving URL: {}", url);
                Optional<String> archiveUrl = archiveFetcher.fetchArchivedSnapshot(url);

                if (archiveUrl.isPresent()) {
                    String archived = archiveUrl.get();
                    log.info("Archived snapshot found for {} -> {}", url, archived);
                    redisTemplate.opsForList().leftPush(KEY_TO_EXTRACT, archived);
                } else {
                    log.warn("No archive found for {}, delegating to retryScheduler", url);
                    // first failure → attempt #1; the scheduler will increment further
                    retryScheduler.scheduleRetry(url, 1);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in archiveWorker", e);
            Thread.currentThread().interrupt();
        }
    }

    public void runExtractWorker(int maxIterations) {
        int iterations = 0;
        try {
            while (!Thread.currentThread().isInterrupted() && iterations++ < maxIterations) {
                String archiveUrl = redisTemplate.opsForList()
                        .rightPop(KEY_TO_EXTRACT, 1, TimeUnit.SECONDS);

                if (archiveUrl == null) continue;

                log.info("Extracting answers from {}", archiveUrl);
                List<String> answers = extractor.extractAnswers(archiveUrl);

                if (!answers.isEmpty()) {
                    String key = "quora:answers:" + archiveUrl.hashCode();
                    redisTemplate.opsForList().leftPushAll(key, answers);
                    redisTemplate.opsForList().leftPush(KEY_SENTIMENT_PENDING, key);
                    log.info("Stored {} answers for {}", answers.size(), archiveUrl);
                } else {
                    log.warn("No answers extracted from {}", archiveUrl);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in extractWorker", e);
            Thread.currentThread().interrupt();
        }
    }
}