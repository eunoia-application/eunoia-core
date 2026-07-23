package ru.eunoia.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.event.DomainEvent;
import ru.eunoia.application.domain.event.UserDeleted;
import ru.eunoia.application.domain.event.UserRegistered;
import ru.eunoia.application.port.out.EventPublisherPort;

/** Публикует доменные события auth в Kafka (JSON). Топик — по типу события, ключ — userId (порядок на юзера). */
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    static final String TOPIC_USER_REGISTERED = "user.registered";
    static final String TOPIC_USER_DELETED = "user.deleted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent event) {
        switch (event) {
            case UserRegistered e -> kafkaTemplate.send(TOPIC_USER_REGISTERED, e.userId().toString(), e);
            case UserDeleted e -> kafkaTemplate.send(TOPIC_USER_DELETED, e.userId().toString(), e);
        }
    }
}
