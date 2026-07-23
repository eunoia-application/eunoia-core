package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

/**
 * Значения топиков читаем как String (StringDeserializer в application.yml), а в тип листенера
 * превращает StringJsonMessageConverter — так один консюмер разбирает разные события по типу
 * параметра метода. Конвертер тянет JavaTimeModule (для occurredAt).
 */
@Configuration
public class KafkaConfig {

    @Bean
    public RecordMessageConverter jsonMessageConverter() {
        return new StringJsonMessageConverter();
    }
}
