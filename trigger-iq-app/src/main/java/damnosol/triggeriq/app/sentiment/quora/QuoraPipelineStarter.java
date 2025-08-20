package damnosol.triggeriq.app.sentiment.quora;

import damnosol.triggeriq.sentiment.quora.QuoraPipelineWorkers;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoraPipelineStarter {

    private final QuoraPipelineWorkers workers;

    @PostConstruct
    public void init() {
        log.info("Auto-starting Quora pipeline workers via @Async...");
        workers.archiveWorker();
        workers.extractWorker();
    }
}