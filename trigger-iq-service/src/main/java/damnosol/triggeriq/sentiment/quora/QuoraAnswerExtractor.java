package damnosol.triggeriq.sentiment.quora;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class QuoraAnswerExtractor {

    @Value("${triggeriq.quora.user-agent}")
    private String userAgent;

    @Value("${triggeriq.quora.timeout-ms}")
    private int timeoutMs;

    private static final String[] ANSWER_SELECTORS = {
            "div.q-relative.spacing_log_answer_content",
            "div.q-box.qu-mb--medium",
            "div.Answer div.q-box"
    };

    /**
     * Fetches Quora answers from an archived URL with automatic retry on IOException.
     */
    @Retryable(
            value = IOException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<String> extractAnswers(String archivedUrl) throws IOException {
        log.info("Fetching archived Quora page: {}", archivedUrl);

        Document doc = Jsoup.connect(archivedUrl)
                .userAgent(userAgent)
                .timeout(timeoutMs)
                .get();

        return extractAnswersFromDocument(doc, archivedUrl);
    }

    /**
     * Fallback when all retries fail.
     */
    @Recover
    public List<String> recover(IOException e, String archivedUrl) {
        log.error("All retries failed for {}. Returning empty list.", archivedUrl, e);
        return List.of();
    }

    /**
     * Extracts answers from a parsed HTML Document.
     * Separated for easier unit testing and reusability.
     */
    List<String> extractAnswersFromDocument(Document doc, String url) {
        List<String> answers = new ArrayList<>();
        Elements answerBlocks = new Elements();

        // Aggregate elements from all selectors
        for (String selector : ANSWER_SELECTORS) {
            Elements found = Optional.of(doc.select(selector)).orElse(new Elements());
            if (!found.isEmpty()) {
                log.debug("Selector [{}] matched {} elements", selector, found.size());
                answerBlocks.addAll(found);
            }
        }

        // Filter valid answers
        for (Element block : answerBlocks) {
            String cleanText = block.text().trim();
            if (isValidAnswer(cleanText)) {
                answers.add(cleanText);
                log.debug("Extracted answer ({} chars) from {}: {}",
                        cleanText.length(),
                        url,
                        cleanText.length() > 120 ? cleanText.substring(0, 120) + "..." : cleanText);
            }
        }

        log.info("Total valid answers extracted from {}: {}", url, answers.size());
        return answers;
    }

    /**
     * Determines if a text block is a valid answer.
     */
    private boolean isValidAnswer(String text) {
        if (text == null || text.trim().length() < 50) return false;
        String lower = text.toLowerCase().trim();

        // Reject only obvious boilerplate, not answers that mention Quora naturally
        return !(lower.startsWith("related questions") || lower.startsWith("see more quora") || lower.startsWith("more answers"));
    }
}