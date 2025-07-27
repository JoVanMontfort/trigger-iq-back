package damnosol.triggeriq.serializer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import damnosol.triggeriq.sentiment.reddit.Post;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.List;

public class ListPostSerializer implements RedisSerializer<List<Post>> {

    private final Jackson2JsonRedisSerializer<List<Post>> serializer;

    @SuppressWarnings("deprecation")
    public ListPostSerializer(ObjectMapper objectMapper) {
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, Post.class);
        this.serializer = new Jackson2JsonRedisSerializer<>(javaType);
        this.serializer.setObjectMapper(objectMapper);
    }

    @Override
    public byte[] serialize(List<Post> posts) throws SerializationException {
        try {
            return serializer.serialize(posts);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize List<Post>", e);
        }
    }

    @Override
    public List<Post> deserialize(byte[] bytes) throws SerializationException {
        try {
            return serializer.deserialize(bytes);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize List<Post>", e);
        }
    }
}