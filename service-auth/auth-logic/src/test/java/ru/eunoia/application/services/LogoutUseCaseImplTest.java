package ru.eunoia.application.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AuthEventRecorder recorder;

    private LogoutUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCaseImpl(refreshTokenRepository, recorder);
    }

    @Test
    void logout_revokesUserTokensAndRecordsLogout() {
        UUID userId = UUID.randomUUID();

        useCase.logout(userId);

        verify(refreshTokenRepository).revokeByUserId(userId);
        verify(recorder).record(userId, AuthEvent.EventType.LOGOUT, true);
        verifyNoMoreInteractions(refreshTokenRepository, recorder);
    }
}
