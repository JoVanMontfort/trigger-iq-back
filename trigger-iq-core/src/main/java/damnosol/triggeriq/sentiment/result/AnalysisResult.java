package damnosol.triggeriq.sentiment.result;

import damnosol.triggeriq.sentiment.reddit.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Core model capturing the key results of sentiment and model analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    // Raw posts processed
    private List<Post> posts;

    // Model performance
    private boolean trained;
    private String modelUsed;
    private double rSquared;
    private Map<String, Double> featureImportance;
    private List<String> predictionSamples;
}