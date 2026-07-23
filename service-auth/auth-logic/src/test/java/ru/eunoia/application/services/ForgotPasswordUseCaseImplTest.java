package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.EmailSenderPort;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseImplTest {

    private static final String EMAIL = "user@eunoia.ru";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Duration TTL = Duration.ofHours(1);

    @Mock
    private UserRepositoryPort users;
    @Mock
    private OneTimeTokenRepositoryPort tokens;
    @Mock
    private EmailSenderPort emailSender;
    @Mock
    private AuthEventRecorder recorder;

    private ForgotPasswordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ForgotPasswordUseCaseImpl(users, tokens, emailSender, recorder, TTL);
    }

    private static User user() {
        return new User(USER_ID, EMAIL, "user", "hash", true, true, false, 0, null,
                LocalDateTime.now().minusDays(1), null, null);
    }

    @Test
    void requestReset_knownEmail_issuesTokenSendsEmailAndRecords() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        useCase.requestReset(EMAIL);

        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordReset(eq(EMAIL), rawToken.capture());

        ArgumentCaptor<OneTimeToken> oneTime = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(tokens).save(oneTime.capture());
        assertThat(oneTime.getValue().userId()).isEqualTo(USER_ID);
        assertThat(oneTime.getValue().purpose()).isEqualTo(OneTimeToken.Purpose.PASSWORD_RESET);
        assertThat(oneTime.getValue().tokenHash()).isEqualTo(OneTimeToken.hash(rawToken.getValue()));

        verify(recorder).record(USER_ID, AuthEvent.EventType.PASSWORD_RESET_REQUESTED, true);
    }

    @Test
    void requestReset_unknownEmail_doesNothing() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        useCase.requestReset(EMAIL);

        verifyNoInteractions(tokens, emailSender, recorder);
    }
}
