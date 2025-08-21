package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoraRetryScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RETRY_QUEUE = "quora:retry:queue";
    private static final String ARCHIVE_QUEUE = "quora:archive:queue";

    /**
     * When a fetch fails, push it here with a delay.
     */
    public void scheduleRetry(String url, int attempt) {
        long delaySeconds = (long) Math.pow(2, attempt); // exponential backoff
        long retryAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);

        log.warn("Scheduling retry for {} in {} seconds (attempt {})", url, delaySeconds, attempt);

        redisTemplate.opsForZSet().add(RETRY_QUEUE, url, retryAt);
    }

    /**
     * Periodically poll for due retries and requeue them.
     */
    @Scheduled(fixedDelay = 10000)
    public void processRetries() {
        long now = System.currentTimeMillis();
        var due = redisTemplate.opsForZSet().rangeByScore(RETRY_QUEUE, 0, now);

        if (due == null || due.isEmpty()) return;

        for (String url : due) {
            log.info("Re-queueing failed URL {}", url);
            redisTemplate.opsForList().rightPush(ARCHIVE_QUEUE, url);
            redisTemplate.opsForZSet().remove(RETRY_QUEUE, url);
        }
    }
}