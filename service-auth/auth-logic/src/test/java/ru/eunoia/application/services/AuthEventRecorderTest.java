package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AuthEventRecorderTest {

    @Mock
    private AuthEventRepositoryPort repository;

    private AuthEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AuthEventRecorder(repository);
    }

    @Test
    void record_buildsEventWithGivenFieldsAndTimestamp() {
        UUID userId = UUID.randomUUID();

        LocalDateTime before = LocalDateTime.now();
        recorder.record(userId, AuthEvent.EventType.LOGIN_SUCCESS, true);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(repository).save(captor.capture());
        AuthEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType()).isEqualTo(AuthEvent.EventType.LOGIN_SUCCESS);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void record_carriesFailureFlagAndEventType() {
        UUID userId = UUID.randomUUID();

        recorder.record(userId, AuthEvent.EventType.LOGIN_FAILED, false);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(repository).save(captor.capture());
        AuthEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType()).isEqualTo(AuthEvent.EventType.LOGIN_FAILED);
        assertThat(event.isSuccess()).isFalse();
    }
}
