package damnosol.triggeriq.sentiment.async.redis.repositories;

import damnosol.triggeriq.sentiment.async.model.responses.SentimentJobResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

@Repository
public class RedisSentimentJobRepository {
    private final RedisTemplate<String, SentimentJobResult> redisTemplate;
    private static final String PREFIX = "sentiment-job:";

    public RedisSentimentJobRepository(@Qualifier("jobResultRedisTemplate") RedisTemplate<String, SentimentJobResult> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String jobId, SentimentJobResult result, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + jobId, result, ttl);
    }

    public SentimentJobResult findById(String jobId) {
        return redisTemplate.opsForValue().get(PREFIX + jobId);
    }

    public void delete(String jobId) {
        redisTemplate.delete(PREFIX + jobId);
    }

    public Set<String> findAllKeys() {
        return redisTemplate.keys(PREFIX + "*");
    }
}