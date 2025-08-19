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

    private static final String KEY_PENDING_URLS = "quora:pending";
    private static final String KEY_TO_EXTRACT = "quora:to_extract";
    private static final String KEY_SENTIMENT_PENDING = "sentiment:pending";

    private final StringRedisTemplate redisTemplate;
    private final QuoraArchiveFetcher archiveFetcher;
    private final QuoraAnswerExtractor extractor;

    @Async("quoraExecutor")
    public void archiveWorker() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Blocking pop with 30s timeout
                String url = redisTemplate.opsForList()
                        .rightPop(KEY_PENDING_URLS, 30, TimeUnit.SECONDS);

                if (url == null) {
                    continue; // no work, loop again
                }

                log.info("Archiving URL: {}", url);
                Optional<String> archiveUrl = archiveFetcher.fetchArchivedSnapshot(url);

                if (archiveUrl.isPresent()) {
                    String archived = archiveUrl.get();
                    log.info("Archived snapshot found for {} -> {}", url, archived);
                    redisTemplate.opsForList().leftPush(KEY_TO_EXTRACT, archived);
                } else {
                    log.warn("No archive found for {}, re-queueing", url);
                    redisTemplate.opsForList().leftPush(KEY_PENDING_URLS, url);
                    // Optionally: track retry count to avoid infinite retries
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in archiveWorker", e);
            Thread.currentThread().interrupt();
        }
    }

    @Async("quoraExecutor")
    public void extractWorker() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Blocking pop with 30s timeout
                String archiveUrl = redisTemplate.opsForList()
                        .rightPop(KEY_TO_EXTRACT, 30, TimeUnit.SECONDS);

                if (archiveUrl == null) {
                    continue; // no work, loop again
                }

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