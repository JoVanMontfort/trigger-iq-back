package damnosol.triggeriq.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private BedrockService bedrockService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> analyze(@RequestBody FeedbackRequest request) {
        FeedbackResponse result = bedrockService.analyzeFeedback(request.text());
        return ResponseEntity.ok(result);
    }
}
