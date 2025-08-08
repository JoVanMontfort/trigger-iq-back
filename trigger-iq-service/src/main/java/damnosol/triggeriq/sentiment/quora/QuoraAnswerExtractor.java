package damnosol.triggeriq.sentiment.quora;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuoraAnswerExtractor {

    public List<String> extractAnswers(String archivedUrl) {
        List<String> answers = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(archivedUrl)
                    .userAgent("Mozilla/5.0 (TriggerIQ-Bot)")
                    .timeout(10000)
                    .get();

            // Quora often wraps answers in divs with rich text formatting
            Elements answerBlocks = doc.select("div.q-relative");

            for (Element block : answerBlocks) {
                // Filter by likely classes that hold full answers (adjust if needed)
                if (block.text().length() > 100) {
                    answers.add(block.text());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return answers;
    }
}