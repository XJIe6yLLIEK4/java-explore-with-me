package ru.practicum.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.common.util.DateTimeUtil;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            JavaTimeModule javaTime = new JavaTimeModule();
            javaTime.addSerializer(java.time.LocalDateTime.class, DateTimeUtil.LDT_SERIALIZER);
            javaTime.addDeserializer(java.time.LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeUtil.FORMATTER));

            builder.modules(javaTime);
            builder.simpleDateFormat(DateTimeUtil.PATTERN);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
