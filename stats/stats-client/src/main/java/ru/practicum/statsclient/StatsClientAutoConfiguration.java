package ru.practicum.statsclient;

import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@EnableConfigurationProperties(StatsClientProperties.class)
public class StatsClientAutoConfiguration {

    @Bean
    RestTemplate statsRestTemplate(RestTemplateBuilder b, StatsClientProperties props) {
        return b
                .rootUri(props.getBaseUrl())
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    StatsClient statsClient(RestTemplate statsRestTemplate) {
        return new StatsClient(statsRestTemplate);
    }
}
