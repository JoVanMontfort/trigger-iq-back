package damnosol.triggeriq.rest.quora;

import damnosol.triggeriq.sentiment.quora.QuoraLinkFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quora")
@RequiredArgsConstructor
@Slf4j
public class QuoraSentimentPipelineController {

    private static final String KEY_PENDING_URLS = "quora:pending";
    private static final String KEY_TO_EXTRACT = "quora:to_extract";
    private static final String KEY_SENTIMENT_PENDING = "sentiment:pending";

    private final StringRedisTemplate redisTemplate;
    private final QuoraLinkFetcher linkFetcher;

    /**
     * Enqueue links for a given keyword.
     */
    @GetMapping("/enqueue")
    public ResponseEntity<String> enqueueQuoraLinks(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().body("Keyword must not be empty");
        }

        log.info("Fetching Quora links for keyword '{}'", keyword);
        List<String> links = linkFetcher.fetchQuoraLinks(keyword);

        if (links.isEmpty()) {
            return ResponseEntity.ok("No links found for keyword: " + keyword);
        }

        links.forEach(link -> redisTemplate.opsForList().leftPush(KEY_PENDING_URLS, link));
        log.info("Enqueued {} links into Redis queue '{}'", links.size(), KEY_PENDING_URLS);

        return ResponseEntity.ok("Enqueued " + links.size() + " links for processing");
    }

    /**
     * Get all available answers currently stored in Redis.
     */
    @GetMapping("/answers")
    public ResponseEntity<Map<String, List<String>>> getAllAnswers() {
        // Find all Redis keys matching quora:answers:*
        Set<String> keys = redisTemplate.keys("quora:answers:*");

        if (keys == null || keys.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        Map<String, List<String>> results = keys.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> Optional.ofNullable(redisTemplate.opsForList().range(key, 0, -1))
                                .orElse(List.of())
                ));

        return ResponseEntity.ok(results);
    }

    /**
     * Queue status endpoint: see how many items are pending at each stage.
     */
    @GetMapping("/queue-status")
    public ResponseEntity<Map<String, Long>> getQueueStatus() {
        Map<String, Long> status = new HashMap<>();
        status.put("pendingUrls", redisTemplate.opsForList().size(KEY_PENDING_URLS));
        status.put("toExtract", redisTemplate.opsForList().size(KEY_TO_EXTRACT));
        status.put("sentimentPending", redisTemplate.opsForList().size(KEY_SENTIMENT_PENDING));
        return ResponseEntity.ok(status);
    }
}
