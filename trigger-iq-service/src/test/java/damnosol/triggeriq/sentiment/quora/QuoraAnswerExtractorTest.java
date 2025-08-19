package damnosol.triggeriq.sentiment.quora;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoraAnswerExtractorTest {

    private QuoraAnswerExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new QuoraAnswerExtractor();
        // Inject config values without Spring context
        ReflectionTestUtils.setField(extractor, "userAgent", "TestAgent");
        ReflectionTestUtils.setField(extractor, "timeoutMs", 5000);
    }

    @Test
    @DisplayName("Should extract valid answers from a well-formed HTML fixture")
    void shouldExtractValidAnswersFromHtmlFixture() throws Exception {
        File input = new File("src/test/resources/quora_sample.html");
        Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name());

        // Act
        List<String> answers = extractor.extractAnswersFromDocument(doc, "test-url");

        // Assert
        assertThat(answers)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1)
                .first()
                .asString()
                .contains("valid Quora answer");
    }

    @Test
    @DisplayName("Should return empty list when no answers are present")
    void shouldReturnEmptyListWhenNoAnswers() throws Exception {
        File input = new File("src/test/resources/quora_empty.html"); // empty fixture
        Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name());

        List<String> answers = extractor.extractAnswersFromDocument(doc, "test-url");

        assertThat(answers)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("Should filter out answers shorter than 50 characters")
    void shouldFilterOutShortAnswers() throws Exception {
        File input = new File("src/test/resources/quora_short.html"); // fixture with short divs
        Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name());

        List<String> answers = extractor.extractAnswersFromDocument(doc, "test-url");

        assertThat(answers)
                .allSatisfy(answer -> assertThat(answer.length()).isGreaterThan(50));
    }
}