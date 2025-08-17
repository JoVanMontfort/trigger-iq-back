package damnosol.triggeriq.rest.quora;

import damnosol.triggeriq.sentiment.quora.QuoraAnswerExtractor;
import damnosol.triggeriq.sentiment.quora.QuoraArchiveFetcher;
import damnosol.triggeriq.sentiment.quora.QuoraLinkFetcher;
import damnosol.triggeriq.sentiment.quora.responses.QuoraAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST controller for fetching Quora answers based on a keyword.
 */
@RestController
@RequestMapping("/api/quora")
@RequiredArgsConstructor
@Slf4j
public class QuoraSentimentPipelineController {

    private final QuoraLinkFetcher linkFetcher;
    private final QuoraArchiveFetcher archiveFetcher;
    private final QuoraAnswerExtractor answerExtractor;

    /**
     * Fetches Quora answers for the given keyword.
     *
     * @param keyword the search keyword to find relevant Quora links
     * @return list of QuoraAnswerResponse with original and archived links plus extracted answers
     */
    @GetMapping("/answers")
    public ResponseEntity<List<QuoraAnswerResponse>> fetchQuoraAnswers(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            log.warn("Received empty or null keyword in fetchQuoraAnswers");
            return ResponseEntity.badRequest().build();
        }

        log.info("Fetching Quora links for keyword '{}'", keyword);
        var links = linkFetcher.fetchQuoraLinks(keyword);

        if (links.isEmpty()) {
            log.info("No Quora links found for keyword '{}'", keyword);
            return ResponseEntity.ok(List.of());
        }

        var results = links.parallelStream()
                .map(link -> {
                    log.debug("Processing link: {}", link);
                    return archiveFetcher.fetchArchivedSnapshot(link)
                            .map(archivedUrl -> {
                                var answers = answerExtractor.extractAnswers(archivedUrl);
                                if (answers.isEmpty()) {
                                    log.debug("No answers found for archived URL {}", archivedUrl);
                                    return null;
                                }
                                return new QuoraAnswerResponse(link, archivedUrl, answers);
                            })
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Returning {} QuoraAnswerResponse(s) for keyword '{}'", results.size(), keyword);
        return ResponseEntity.ok(results);
    }
}
