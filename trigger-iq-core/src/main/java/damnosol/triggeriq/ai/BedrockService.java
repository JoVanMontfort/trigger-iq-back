package damnosol.triggeriq.ai;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

@Service
public class BedrockService {

    private final BedrockRuntimeClient bedrockClient;

    public BedrockService() {
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.EU_WEST_3)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public FeedbackResponse analyzeFeedback(String inputText) {
        String prompt = String.format("""
                    Analyze the following feedback. 
                    Return sentiment (positive, neutral, or negative) and a short resolution suggestion.
                
                    Feedback: \"%s\"
                    Format:
                    {{
                      \"sentiment\": \"...\",
                      \"resolution\": \"..."
                    }}
                """, inputText);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId("anthropic.claude-3-sonnet-20240229-v1:0")
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String("{\"prompt\": \"" + prompt + "\", \"max_tokens\": 300}"))
                .build();

        var response = bedrockClient.invokeModel(request);
        String jsonOutput = response.body().asUtf8String();

        String sentiment = jsonOutput.replaceAll(".*\"sentiment\":\s*\"(.*?)\".*", "$1");
        String resolution = jsonOutput.replaceAll(".*\"resolution\":\s*\"(.*?)\".*", "$1");

        return new FeedbackResponse(sentiment, resolution);
    }
}
