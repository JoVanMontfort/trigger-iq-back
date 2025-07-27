package damnosol.triggeriq.rest.reddit;

import damnosol.triggeriq.jobs.SentimentJobService;
import damnosol.triggeriq.sentiment.async.model.requests.SentimentJobRequest;
import damnosol.triggeriq.sentiment.async.model.responses.SentimentJobResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sentiment-job")
public class SentimentJobController {

    private final SentimentJobService jobService;

    public SentimentJobController(SentimentJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitJob(@RequestBody SentimentJobRequest request) {
        String jobId = jobService.submitJob(request);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<SentimentJobResult> getStatus(@PathVariable String jobId) {
        SentimentJobResult result = jobService.getJobStatus(jobId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(result);
    }
}