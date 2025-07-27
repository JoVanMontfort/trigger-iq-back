package damnosol.triggeriq.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import damnosol.triggeriq.sentiment.reddit.Comment;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListCommentSerializer implements RedisSerializer<List<Comment>> {

    private final ObjectMapper mapper;

    public ListCommentSerializer(ObjectMapper mapper) {
        this.mapper = mapper.copy(); // To avoid mutating shared ObjectMapper
    }

    @Override
    public byte[] serialize(List<Comment> comments) throws SerializationException {
        try {
            return mapper.writeValueAsBytes(comments);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing List<Comment>", e);
        }
    }

    @Override
    public List<Comment> deserialize(byte[] bytes) throws SerializationException {
        try {
            if (bytes == null || bytes.length == 0) return new ArrayList<>();
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Comment.class);
            return mapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing List<Comment>", e);
        }
    }
}
