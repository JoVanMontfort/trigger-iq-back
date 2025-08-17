package damnosol.triggeriq.sentiment.quora;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class QuoraAnswerExtractor {

    private static final String USER_AGENT = "Mozilla/5.0 (TriggerIQ-Bot)";

    public List<String> extractAnswers(String archivedUrl) {
        List<String> answers = new ArrayList<>();
        try {
            log.info("Fetching archived Quora page: {}", archivedUrl);

            Document doc = Jsoup.connect(archivedUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            Elements answerBlocks = doc.select(
                    "div.q-relative.spacing_log_answer_content, " +
                            "div.q-box.qu-mb--medium, " +
                            "div.Answer div.q-box"
            );

            for (Element block : answerBlocks) {
                String cleanText = block.text().trim();
                if (cleanText.length() > 50 &&
                        !cleanText.toLowerCase().contains("quora") &&
                        !cleanText.toLowerCase().startsWith("related questions")) {
                    answers.add(cleanText);
                    log.debug("Extracted answer: {}", cleanText.length() > 120
                            ? cleanText.substring(0, 120) + "..."
                            : cleanText);
                }
            }

            log.info("Total answers extracted from {}: {}", archivedUrl, answers.size());

        } catch (IOException e) {
            log.error("Failed to extract answers from archived Quora page: {}", archivedUrl, e);
        }

        return answers;
    }
}