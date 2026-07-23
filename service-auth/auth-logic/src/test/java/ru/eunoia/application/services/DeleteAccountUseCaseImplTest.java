package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.event.UserDeleted;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.out.EventPublisherPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Удаление аккаунта: гасим токены, убираем учётку, пишем аудит и публикуем UserDeleted. */
@ExtendWith(MockitoExtension.class)
class DeleteAccountUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private EventPublisherPort eventPublisher;
    @Mock
    private AuthEventRecorder recorder;

    private DeleteAccountUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteAccountUseCaseImpl(userRepository, refreshTokenRepository, eventPublisher, recorder);
    }

    @Test
    void delete_revokesTokens_removesIdentity_records_andPublishesUserDeleted() {
        UUID userId = UUID.randomUUID();

        useCase.delete(userId);

        verify(refreshTokenRepository).revokeByUserId(userId);
        verify(userRepository).deleteById(userId);
        verify(recorder).record(userId, AuthEvent.EventType.ACCOUNT_DELETED, true);

        ArgumentCaptor<UserDeleted> published = ArgumentCaptor.forClass(UserDeleted.class);
        verify(eventPublisher).publish(published.capture());
        assertThat(published.getValue().userId()).isEqualTo(userId);
        assertThat(published.getValue().occurredAt()).isNotNull();
    }
}
