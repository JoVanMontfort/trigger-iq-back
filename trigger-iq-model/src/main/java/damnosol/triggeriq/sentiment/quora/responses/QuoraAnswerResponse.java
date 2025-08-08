package damnosol.triggeriq.sentiment.quora.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuoraAnswerResponse {
    private String originalUrl;
    private String archivedUrl;
    private List<String> answers;
}