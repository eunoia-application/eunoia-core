package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseImplTest {

    private static final String EMAIL = "user@eunoia.ru";
    private static final String PASSWORD = "secret";
    private static final String HASH = "hashed-secret";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AuthEventRecorder recorder;

    private LoginUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCaseImpl(userRepository, passwordEncoder, tokenProvider,
                refreshTokenRepository, recorder, MAX_ATTEMPTS, LOCK);
    }

    private static User activeUser(int failedAttempts) {
        return new User(USER_ID, EMAIL, "user", HASH, true, true, false, failedAttempts, null,
                LocalDateTime.now().minusDays(1), null, null);
    }

    private static AuthTokens tokens() {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokens("access-token", "refresh-token-value", AuthTokens.BEARER,
                900, USER_ID, now, now.plusMinutes(15), now.plusDays(30));
    }

    @Test
    void login_success_resetsAttemptsSavesRefreshAndRecordsSuccess() {
        AuthTokens tokens = tokens();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser(2)));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateTokens(any(User.class))).thenReturn(tokens);

        Authentication result = useCase.login(new LoginCommand(EMAIL, PASSWORD));

        assertThat(result.user().id()).isEqualTo(USER_ID);
        assertThat(result.user().failedLoginAttempts()).isZero();
        assertThat(result.user().locked()).isFalse();
        assertThat(result.user().lastLoginAt()).isNotNull();
        assertThat(result.tokens()).isSameAs(tokens);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().failedLoginAttempts()).isZero();
        assertThat(savedUser.getValue().locked()).isFalse();

        ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(savedToken.capture());
        assertThat(savedToken.getValue().userId()).isEqualTo(USER_ID);
        assertThat(savedToken.getValue().tokenHash()).isEqualTo(RefreshToken.hash(tokens.refreshToken()));
        assertThat(savedToken.getValue().revoked()).isFalse();

        verify(recorder).record(USER_ID, AuthEvent.EventType.LOGIN_SUCCESS, true);
        verifyNoMoreInteractions(recorder);
    }

    @Test
    void login_userNotFound_throwsAndTouchesNothingElse() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login(new LoginCommand(EMAIL, PASSWORD)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, tokenProvider, refreshTokenRepository, recorder);
    }

    @Test
    void login_wrongPassword_incrementsAttemptRecordsFailedAndThrows() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser(2)));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(new LoginCommand(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().failedLoginAttempts()).isEqualTo(3);

        verify(recorder).record(USER_ID, AuthEvent.EventType.LOGIN_FAILED, false);
        verifyNoMoreInteractions(recorder);
        verifyNoInteractions(tokenProvider, refreshTokenRepository);
    }

    @Test
    void login_lockedAccount_throwsBeforePasswordCheck() {
        User locked = new User(USER_ID, EMAIL, "user", HASH, true, true, true, 0,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().minusDays(1), null, null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> useCase.login(new LoginCommand(EMAIL, PASSWORD)))
                .isInstanceOf(AccountLockedException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, tokenProvider, refreshTokenRepository, recorder);
    }

    @Test
    void login_inactiveAccount_throwsBeforePasswordCheck() {
        User inactive = new User(USER_ID, EMAIL, "user", HASH, true, false, false, 0,
                null, LocalDateTime.now().minusDays(1), null, null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.login(new LoginCommand(EMAIL, PASSWORD)))
                .isInstanceOf(AccountNotActiveException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, tokenProvider, refreshTokenRepository, recorder);
    }
}
