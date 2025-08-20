package damnosol.triggeriq.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "quoraExecutor")
    public Executor quoraExecutor() {
        log.info("Initializing quoraExecutor bean...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        log.debug("Setting core pool size to 2");
        executor.setCorePoolSize(2);

        log.debug("Setting max pool size to 4");
        executor.setMaxPoolSize(4);

        log.debug("Setting queue capacity to 100");
        executor.setQueueCapacity(100);

        log.debug("Setting thread name prefix to 'QuoraWorker-'");
        executor.setThreadNamePrefix("QuoraWorker-");

        executor.initialize();
        log.info("quoraExecutor bean initialized successfully");

        return executor;
    }
}