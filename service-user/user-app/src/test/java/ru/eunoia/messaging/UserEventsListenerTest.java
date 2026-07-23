package ru.eunoia.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.port.in.ProfileLifecycleUseCase;
import ru.eunoia.messaging.event.UserDeletedEvent;
import ru.eunoia.messaging.event.UserRegisteredEvent;

/** Листенер только распаковывает событие и делегирует нужные поля в жизненный цикл профиля. */
@ExtendWith(MockitoExtension.class)
class UserEventsListenerTest {

    @Mock
    private ProfileLifecycleUseCase lifecycle;

    private UserEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserEventsListener(lifecycle);
    }

    @Test
    void onUserRegistered_passesUserFieldsToLifecycle() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event =
                new UserRegisteredEvent(userId, "user@eunoia.ru", "user", LocalDateTime.now());

        listener.onUserRegistered(event);

        verify(lifecycle).onUserRegistered(userId, "user@eunoia.ru", "user");
        verifyNoMoreInteractions(lifecycle);
    }

    @Test
    void onUserDeleted_passesUserIdToLifecycle() {
        UUID userId = UUID.randomUUID();
        UserDeletedEvent event = new UserDeletedEvent(userId, LocalDateTime.now());

        listener.onUserDeleted(event);

        verify(lifecycle).onUserDeleted(userId);
        verifyNoMoreInteractions(lifecycle);
    }
}
