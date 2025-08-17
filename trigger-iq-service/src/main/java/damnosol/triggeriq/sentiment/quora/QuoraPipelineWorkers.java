package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoraPipelineWorkers {

    private final StringRedisTemplate redisTemplate;
    private final QuoraArchiveFetcher archiveFetcher;
    private final QuoraAnswerExtractor extractor;

    @Async
    public void archiveWorker() {
        String url;
        while ((url = redisTemplate.opsForList().rightPop("quora:pending")) != null) {
            Optional<String> archiveUrl = archiveFetcher.fetchArchivedSnapshot(url);
            String finalUrl = url;
            archiveUrl.ifPresentOrElse(
                    archived -> redisTemplate.opsForList().leftPush("quora:to_extract", archived),
                    () -> redisTemplate.opsForList().leftPush("quora:pending", finalUrl)
            );
        }
    }

    @Async
    public void extractWorker() {
        String archiveUrl;
        while ((archiveUrl = redisTemplate.opsForList().rightPop("quora:to_extract")) != null) {
            List<String> answers = extractor.extractAnswers(archiveUrl);
            if (!answers.isEmpty()) {
                String key = "quora:answers:" + archiveUrl.hashCode();
                redisTemplate.opsForList().leftPushAll(key, answers);
                redisTemplate.opsForList().leftPush("sentiment:pending", key);
            }
        }
    }
}