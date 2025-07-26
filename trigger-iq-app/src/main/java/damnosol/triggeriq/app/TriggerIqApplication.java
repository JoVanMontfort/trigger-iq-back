package damnosol.triggeriq.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"damnosol.triggeriq.sentiment.reddit", "damnosol.triggeriq.sentiment", "damnosol.triggeriq.config"})
public class TriggerIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriggerIqApplication.class, args);
    }

}
