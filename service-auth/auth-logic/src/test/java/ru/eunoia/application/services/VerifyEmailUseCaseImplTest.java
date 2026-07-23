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
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class VerifyEmailUseCaseImplTest {

    private static final String RAW = "raw-verify-token";
    private static final String EMAIL = "user@eunoia.ru";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private OneTimeTokenRepositoryPort tokens;
    @Mock
    private UserRepositoryPort users;
    @Mock
    private AuthEventRecorder recorder;

    private VerifyEmailUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyEmailUseCaseImpl(tokens, users, recorder);
    }

    private static OneTimeToken activeToken(Purpose purpose) {
        return new OneTimeToken(UUID.randomUUID(), USER_ID, purpose, OneTimeToken.hash(RAW),
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1), false, null);
    }

    private static User user() {
        return new User(USER_ID, EMAIL, "user", "hash", false, true, false, 0, null,
                LocalDateTime.now().minusDays(1), null, null);
    }

    @Test
    void verify_success_marksEmailVerifiedUsesTokenAndRecords() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.EMAIL_VERIFICATION)));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user()));

        useCase.verify(RAW);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(savedUser.capture());
        assertThat(savedUser.getValue().emailVerified()).isTrue();

        ArgumentCaptor<OneTimeToken> savedToken = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(tokens).save(savedToken.capture());
        assertThat(savedToken.getValue().used()).isTrue();

        verify(recorder).record(USER_ID, AuthEvent.EventType.EMAIL_VERIFICATION_SUCCESS, true);
    }

    @Test
    void verify_tokenNotFound_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.verify(RAW))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, recorder);
    }

    @Test
    void verify_wrongPurpose_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.PASSWORD_RESET)));

        assertThatThrownBy(() -> useCase.verify(RAW))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, recorder);
    }

    @Test
    void verify_inactiveToken_throws() {
        OneTimeToken expired = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.EMAIL_VERIFICATION,
                OneTimeToken.hash(RAW), LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1), false, null);
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.verify(RAW))
                .isInstanceOf(TokenValidationException.class);

        verify(tokens, never()).save(any());
        verifyNoInteractions(users, recorder);
    }

    @Test
    void verify_userMissing_throws() {
        when(tokens.findByTokenHash(OneTimeToken.hash(RAW)))
                .thenReturn(Optional.of(activeToken(Purpose.EMAIL_VERIFICATION)));
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.verify(RAW))
                .isInstanceOf(UserNotFoundException.class);

        verify(users, never()).save(any());
        verify(tokens, never()).save(any());
        verifyNoInteractions(recorder);
    }
}
