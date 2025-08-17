package damnosol.triggeriq.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"damnosol.triggeriq.sentiment.reddit",
        "damnosol.triggeriq.sentiment.quora",
        "damnosol.triggeriq.sentiment",
        "damnosol.triggeriq.config",
        "damnosol.triggeriq.rest.reddit",
        "damnosol.triggeriq.rest.quora",
        "damnosol.triggeriq.jobs"})
public class TriggerIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriggerIqApplication.class, args);
    }

}