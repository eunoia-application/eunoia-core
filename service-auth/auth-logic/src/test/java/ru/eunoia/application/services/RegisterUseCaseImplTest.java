package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.EmailSenderPort;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseImplTest {

    private static final String EMAIL = "new@eunoia.ru";
    private static final String PASSWORD = "secret";
    private static final String USERNAME = "newbie";
    private static final String HASH = "hashed-secret";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Duration EMAIL_TTL = Duration.ofHours(24);

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private OneTimeTokenRepositoryPort oneTimeTokenRepository;
    @Mock
    private EmailSenderPort emailSender;
    @Mock
    private AuthEventRecorder recorder;

    private RegisterUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUseCaseImpl(userRepository, passwordEncoder, tokenProvider,
                refreshTokenRepository, oneTimeTokenRepository, emailSender, recorder, EMAIL_TTL);
    }

    private static User savedUser() {
        LocalDateTime now = LocalDateTime.now();
        return new User(USER_ID, EMAIL, USERNAME, HASH, false, true, false, 0, null, now, now, null);
    }

    private static AuthTokens tokens() {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokens("access-token", "refresh-token-value", AuthTokens.BEARER,
                900, USER_ID, now, now.plusMinutes(15), now.plusDays(30));
    }

    @Test
    void register_success_savesUserRefreshRecordsAndSendsVerification() {
        User saved = savedUser();
        AuthTokens tokens = tokens();
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(tokenProvider.generateTokens(saved)).thenReturn(tokens);

        Authentication result = useCase.register(new RegisterCommand(EMAIL, PASSWORD, USERNAME));

        assertThat(result.user()).isSameAs(saved);
        assertThat(result.tokens()).isSameAs(tokens);

        ArgumentCaptor<User> newUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(newUser.capture());
        assertThat(newUser.getValue().id()).isNull();
        assertThat(newUser.getValue().email()).isEqualTo(EMAIL);
        assertThat(newUser.getValue().username()).isEqualTo(USERNAME);
        assertThat(newUser.getValue().passwordHash()).isEqualTo(HASH);
        assertThat(newUser.getValue().active()).isTrue();
        assertThat(newUser.getValue().emailVerified()).isFalse();

        ArgumentCaptor<RefreshToken> refresh = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refresh.capture());
        assertThat(refresh.getValue().userId()).isEqualTo(USER_ID);
        assertThat(refresh.getValue().tokenHash()).isEqualTo(RefreshToken.hash(tokens.refreshToken()));

        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailVerification(eq(EMAIL), rawToken.capture());
        ArgumentCaptor<OneTimeToken> oneTime = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(oneTimeTokenRepository).save(oneTime.capture());
        assertThat(oneTime.getValue().userId()).isEqualTo(USER_ID);
        assertThat(oneTime.getValue().purpose()).isEqualTo(OneTimeToken.Purpose.EMAIL_VERIFICATION);
        assertThat(oneTime.getValue().tokenHash()).isEqualTo(OneTimeToken.hash(rawToken.getValue()));

        verify(recorder).record(USER_ID, AuthEvent.EventType.REGISTRATION_SUCCESS, true);
        verify(recorder).record(USER_ID, AuthEvent.EventType.EMAIL_VERIFICATION_SENT, true);
        verifyNoMoreInteractions(recorder);
    }

    @Test
    void register_existingEmail_throwsAndTouchesNothingElse() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(new RegisterCommand(EMAIL, PASSWORD, USERNAME)))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, tokenProvider, refreshTokenRepository,
                oneTimeTokenRepository, emailSender, recorder);
    }
}
