package damnosol.triggeriq.sentiment.quora;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QuoraPipelineWorkersTest {

    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOps;
    private QuoraArchiveFetcher archiveFetcher;
    private QuoraAnswerExtractor extractor;
    private QuoraRetryScheduler retryScheduler;
    private QuoraPipelineWorkers workers;

    @BeforeEach
    void setup() {
        redisTemplate = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);

        archiveFetcher = mock(QuoraArchiveFetcher.class);
        extractor = mock(QuoraAnswerExtractor.class);
        retryScheduler = mock(QuoraRetryScheduler.class);

        workers = new QuoraPipelineWorkers(redisTemplate, archiveFetcher, extractor, retryScheduler);
    }

    @Test
    void archiveWorker_success_pushesToExtractQueue_andStops() {
        String url = "https://quora.com/test";
        String archivedUrl = "https://web.archive.org/web/20240101/test";

        when(listOps.rightPop(QuoraPipelineWorkers.KEY_PENDING_URLS, 1, TimeUnit.SECONDS))
                .thenReturn(url)   // first iteration processes work
                .thenReturn(null); // second returns null → loop ends (maxIterations=2)
        when(archiveFetcher.fetchArchivedSnapshot(url)).thenReturn(Optional.of(archivedUrl));

        workers.runArchiveWorker(2);

        verify(listOps).leftPush(QuoraPipelineWorkers.KEY_TO_EXTRACT, archivedUrl);
        verifyNoInteractions(retryScheduler);
    }

    @Test
    void archiveWorker_failure_delegatesToRetryScheduler() {
        String url = "https://quora.com/fail";

        when(listOps.rightPop(QuoraPipelineWorkers.KEY_PENDING_URLS, 1, TimeUnit.SECONDS))
                .thenReturn(url)
                .thenReturn(null);
        when(archiveFetcher.fetchArchivedSnapshot(url)).thenReturn(Optional.empty());

        workers.runArchiveWorker(2);

        verify(retryScheduler).scheduleRetry(url, 1);
        verify(listOps, never()).leftPush(eq(QuoraPipelineWorkers.KEY_TO_EXTRACT), anyString());
    }

    @Test
    void extractWorker_storesAnswers_and_enqueuesSentiment() throws IOException {
        String archivedUrl = "https://web.archive.org/web/20240101/test";
        List<String> answers = List.of("A1", "A2");

        when(listOps.rightPop(QuoraPipelineWorkers.KEY_TO_EXTRACT, 1, TimeUnit.SECONDS))
                .thenReturn(archivedUrl)
                .thenReturn(null);
        when(extractor.extractAnswers(archivedUrl)).thenReturn(answers);

        workers.runExtractWorker(2);

        String key = "quora:answers:" + archivedUrl.hashCode();
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);

        verify(listOps).leftPushAll(eq(key), captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(answers);

        verify(listOps).leftPush("sentiment:pending", key);
    }

    @Test
    void extractWorker_noAnswers_pushesNothing() throws IOException {
        String archivedUrl = "https://web.archive.org/web/20240101/empty";
        when(listOps.rightPop(QuoraPipelineWorkers.KEY_TO_EXTRACT, 1, TimeUnit.SECONDS))
                .thenReturn(archivedUrl)
                .thenReturn(null);
        when(extractor.extractAnswers(archivedUrl)).thenReturn(List.of());

        workers.runExtractWorker(2);

        String key = "quora:answers:" + archivedUrl.hashCode();
        verify(listOps, never()).leftPushAll(eq(key), anyList());
        verify(listOps, never()).leftPush(eq("sentiment:pending"), anyString());
    }
}