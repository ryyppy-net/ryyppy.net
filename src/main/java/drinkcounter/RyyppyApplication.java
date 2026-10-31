package drinkcounter;

import org.springframework.boot.SpringApplication;
import drinkcounter.analytics.PostHogProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PostHogProperties.class)
public class RyyppyApplication {
    public static void main(String[] args) {
        SpringApplication.run(RyyppyApplication.class, args);
    }
}
