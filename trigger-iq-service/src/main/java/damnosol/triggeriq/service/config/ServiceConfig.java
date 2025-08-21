package damnosol.triggeriq.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ServiceConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
