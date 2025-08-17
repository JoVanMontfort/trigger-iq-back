package damnosol.triggeriq.sentiment.quora;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuoraScheduledRetries {

    private final QuoraPipelineWorkers workers;

    @Scheduled(fixedDelay = 3600000) // every hour
    public void retryPending() {
        workers.archiveWorker();
    }
}