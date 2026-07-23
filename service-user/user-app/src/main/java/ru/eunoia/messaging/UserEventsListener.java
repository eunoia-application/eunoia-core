package ru.eunoia.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.in.ProfileLifecycleUseCase;
import ru.eunoia.messaging.event.UserDeletedEvent;
import ru.eunoia.messaging.event.UserRegisteredEvent;

/** Слушает события auth и ведёт жизненный цикл профиля (create/delete). */
@Component
@RequiredArgsConstructor
public class UserEventsListener {

    private final ProfileLifecycleUseCase lifecycle;

    @KafkaListener(topics = "user.registered")
    public void onUserRegistered(UserRegisteredEvent event) {
        lifecycle.onUserRegistered(event.userId(), event.email(), event.username());
    }

    @KafkaListener(topics = "user.deleted")
    public void onUserDeleted(UserDeletedEvent event) {
        lifecycle.onUserDeleted(event.userId());
    }
}
