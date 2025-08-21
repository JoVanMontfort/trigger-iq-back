package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoraRetryScheduler {

    private static final String RETRY_KEY = "quora:retry";
    private static final String KEY_PENDING_URLS = "quora:pending";
    private static final String DEAD_LETTER_KEY = "quora:dead";
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;

    /**
     * Store a failed URL with next retry time.
     */
    public void scheduleRetry(String url, int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            log.error("Giving up on {} after {} attempts, moving to dead-letter queue", url, attempt - 1);
            redisTemplate.opsForList().leftPush(DEAD_LETTER_KEY, url);
            return;
        }

        long delaySeconds = (long) Math.pow(2, attempt); // exponential backoff
        long retryAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);

        redisTemplate.opsForZSet().add(RETRY_KEY, url + "|" + attempt, retryAt);
        log.info("Scheduled retry #{} for {} at +{}s", attempt, url, delaySeconds);
    }

    /**
     * Periodically check retries and requeue expired ones.
     */
    @Scheduled(fixedDelay = 5000)
    public void processRetries() {
        long now = System.currentTimeMillis();
        Set<String> due = redisTemplate.opsForZSet()
                .rangeByScore(RETRY_KEY, 0, now);

        if (due == null || due.isEmpty()) {
            return;
        }

        for (String entry : due) {
            redisTemplate.opsForZSet().remove(RETRY_KEY, entry);
            String[] parts = entry.split("\\|");
            String url = parts[0];
            int attempt = Integer.parseInt(parts[1]);

            log.info("Retrying {} (attempt #{})", url, attempt);
            redisTemplate.opsForList().leftPush(KEY_PENDING_URLS, url);

            // next retry scheduled here
            scheduleRetry(url, attempt + 1);
        }
    }
}