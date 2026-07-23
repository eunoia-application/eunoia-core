package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.OneTimeToken.Purpose;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseImplTest {

    private static final String RAW = "raw-reset-token";
    private static final String NEW_PASSWORD = "new-password";
    private static final String NEW_HASH = "new-hash";
    private static final String EMAIL = "user@eunoia.ru";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private OneTimeTokenRepositoryPort tokens;
    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private RefreshTokenRepositoryPort refreshTokens;
    @Mock
    private AuthEventRecorder recorder;

    private ResetPasswordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResetPasswordUseCaseImpl(tokens, users, passwordEncoder, refreshTokens, recorder);
    }

    private static OneTimeToken activeToken(Purpose purpose) {
        return new OneTimeToken(UUID.randomUUID(), USER_ID, purpose, OneTimeToken.hash(RAW),
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1), false, null);
    }

    private static User user() {
        return new User(USER_ID, EMAIL, "user", "old-hash", true, true, false, 0, null,
                LocalDateTime.now().minusDays(1), null, null);
    }

    @Test
    void reset_success_changesPasswordUsesTokenRevokesSessionsAndRecords() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.PASSWORD_RESET)));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        useCase.reset(RAW, NEW_PASSWORD);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(savedUser.capture());
        assertThat(savedUser.getValue().passwordHash()).isEqualTo(NEW_HASH);

        ArgumentCaptor<OneTimeToken> savedToken = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(tokens).save(savedToken.capture());
        assertThat(savedToken.getValue().used()).isTrue();

        verify(refreshTokens).revokeByUserId(USER_ID);
        verify(recorder).record(USER_ID, AuthEvent.EventType.PASSWORD_RESET_SUCCESS, true);
    }

    @Test
    void reset_tokenNotFound_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reset(RAW, NEW_PASSWORD))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, passwordEncoder, refreshTokens, recorder);
    }

    @Test
    void reset_wrongPurpose_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.EMAIL_VERIFICATION)));

        assertThatThrownBy(() -> useCase.reset(RAW, NEW_PASSWORD))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, passwordEncoder, refreshTokens, recorder);
    }

    @Test
    void reset_inactiveToken_throws() {
        OneTimeToken used = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.PASSWORD_RESET,
                OneTimeToken.hash(RAW), LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusHours(1), true, LocalDateTime.now().minusMinutes(1));
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW))).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> useCase.reset(RAW, NEW_PASSWORD))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, passwordEncoder, refreshTokens, recorder);
    }

    @Test
    void reset_userMissing_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.PASSWORD_RESET)));
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reset(RAW, NEW_PASSWORD))
                .isInstanceOf(UserNotFoundException.class);

        verify(users, never()).save(any());
        verify(tokens, never()).save(any());
        verifyNoInteractions(passwordEncoder, refreshTokens, recorder);
    }
}
