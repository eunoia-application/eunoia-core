package ru.eunoia.messaging;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.eunoia.application.domain.event.UserDeleted;
import ru.eunoia.application.domain.event.UserRegistered;

/** Событие уходит в топик по своему типу, ключ = userId (гарантирует порядок событий на пользователя). */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaEventPublisherAdapter(kafkaTemplate);
    }

    @Test
    void publishes_userRegistered_to_registered_topic() {
        UUID userId = UUID.randomUUID();
        UserRegistered event = new UserRegistered(userId, "user@example.com", "eunoia_user", LocalDateTime.now());

        adapter.publish(event);

        verify(kafkaTemplate).send(KafkaEventPublisherAdapter.TOPIC_USER_REGISTERED, userId.toString(), event);
    }

    @Test
    void publishes_userDeleted_to_deleted_topic() {
        UUID userId = UUID.randomUUID();
        UserDeleted event = new UserDeleted(userId, LocalDateTime.now());

        adapter.publish(event);

        verify(kafkaTemplate).send(KafkaEventPublisherAdapter.TOPIC_USER_DELETED, userId.toString(), event);
    }
}
