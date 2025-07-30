package damnosol.triggeriq.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import damnosol.triggeriq.sentiment.async.model.responses.SentimentJobResult;
import damnosol.triggeriq.sentiment.reddit.Comment;
import damnosol.triggeriq.sentiment.reddit.Post;
import damnosol.triggeriq.serializer.ListCommentSerializer;
import damnosol.triggeriq.serializer.ListPostSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class RedisConfig {

    /**
     * Reusable ObjectMapper configured for Java time and manual use.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Support for java.time.Instant and other temporal classes
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
            }
        });

        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * RedisTemplate for SentimentJobResult caching.
     */
    @Bean(name = "jobResultRedisTemplate")
    public RedisTemplate<String, SentimentJobResult> jobResultRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper
    ) {
        RedisTemplate<String, SentimentJobResult> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<SentimentJobResult> valueSerializer =
                new Jackson2JsonRedisSerializer<>(SentimentJobResult.class);
        valueSerializer.setObjectMapper(redisObjectMapper);

        template.setValueSerializer(valueSerializer);
        return template;
    }

    /**
     * RedisTemplate for List<Post>.
     */
    @Bean(name = "postListRedisTemplate")
    public RedisTemplate<String, List<Post>> postListRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper
    ) {
        RedisTemplate<String, List<Post>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ListPostSerializer(redisObjectMapper));
        return template;
    }

    /**
     * RedisTemplate for List<Comment>.
     */
    @Bean(name = "commentListRedisTemplate")
    public RedisTemplate<String, List<Comment>> commentListRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper
    ) {
        RedisTemplate<String, List<Comment>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ListCommentSerializer(redisObjectMapper));
        return template;
    }
}