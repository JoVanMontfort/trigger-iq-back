package damnosol.triggeriq.sentiment.async.model.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentJobRequest {
    private String subreddit;
    private Integer limit;
    private List<String> keywords;
    private Integer minUpvotes;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;
    private List<String> authors;
}