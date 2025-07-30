package damnosol.triggeriq.rest.quora;

import damnosol.triggeriq.sentiment.quora.QuoraAnswerExtractor;
import damnosol.triggeriq.sentiment.quora.QuoraArchiveFetcher;
import damnosol.triggeriq.sentiment.quora.QuoraLinkFetcher;
import damnosol.triggeriq.sentiment.quora.responses.QuoraAnswerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/quora")
@RequiredArgsConstructor
public class QuoraSentimentPipelineController {

    private final QuoraLinkFetcher linkFetcher;
    private final QuoraArchiveFetcher archiveFetcher;
    private final QuoraAnswerExtractor answerExtractor;

    @GetMapping("/answers")
    public ResponseEntity<List<QuoraAnswerResponse>> fetchQuoraAnswers(@RequestParam String keyword) {
        List<String> links = linkFetcher.fetchQuoraLinks(keyword);
        List<QuoraAnswerResponse> results = new ArrayList<>();

        for (String link : links) {
            archiveFetcher.fetchArchivedSnapshot(link).ifPresent(archivedUrl -> {
                List<String> answers = answerExtractor.extractAnswers(archivedUrl);
                if (!answers.isEmpty()) {
                    results.add(new QuoraAnswerResponse(link, archivedUrl, answers));
                }
            });
        }

        return ResponseEntity.ok(results);
    }
}