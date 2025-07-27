package damnosol.triggeriq.sentiment.async.model.responses;

import damnosol.triggeriq.sentiment.async.model.status.JobStatus;
import damnosol.triggeriq.sentiment.dto.SentimentAnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentJobResult {
    private JobStatus status;
    private SentimentAnalysisResult result;
    private String error;
    private OffsetDateTime timestamp;
}