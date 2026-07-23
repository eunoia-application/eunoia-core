package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseImplTest {

    private static final String RAW = "raw-refresh-token";
    private static final String EMAIL = "user@eunoia.ru";
    private static final String HASH = "hashed-secret";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private TokenProviderPort tokenProvider;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AuthEventRecorder recorder;

    private RefreshTokenUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCaseImpl(userRepository, tokenProvider, refreshTokenRepository, recorder);
    }

    private static RefreshToken activeStored() {
        return new RefreshToken(UUID.randomUUID(), USER_ID, RefreshToken.hash(RAW),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), false, null);
    }

    private static User activeUser() {
        return new User(USER_ID, EMAIL, "user", HASH, true, true, false, 0, null,
                LocalDateTime.now().minusDays(1), null, null);
    }

    private static AuthTokens tokens() {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokens("access-token", "new-refresh-token", AuthTokens.BEARER,
                900, USER_ID, now, now.plusMinutes(15), now.plusDays(30));
    }

    @Test
    void refresh_success_rotatesTokensAndRecordsRefresh() {
        RefreshToken stored = activeStored();
        User user = activeUser();
        AuthTokens tokens = tokens();
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash(RAW))).thenReturn(Optional.of(stored));
        when(tokenProvider.extractUserId(RAW)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokens(user)).thenReturn(tokens);

        Authentication result = useCase.refresh(RAW);

        assertThat(result.user()).isSameAs(user);
        assertThat(result.tokens()).isSameAs(tokens);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(saved.capture());
        List<RefreshToken> all = saved.getAllValues();
        // old token revoked
        assertThat(all.get(0).revoked()).isTrue();
        assertThat(all.get(0).tokenHash()).isEqualTo(stored.tokenHash());
        // brand-new token issued
        assertThat(all.get(1).revoked()).isFalse();
        assertThat(all.get(1).userId()).isEqualTo(USER_ID);
        assertThat(all.get(1).tokenHash()).isEqualTo(RefreshToken.hash(tokens.refreshToken()));

        verify(recorder).record(USER_ID, AuthEvent.EventType.TOKEN_REFRESH, true);
    }

    @Test
    void refresh_invalidToken_throwsWithoutTouchingStores() {
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(false);

        assertThatThrownBy(() -> useCase.refresh(RAW))
                .isInstanceOf(TokenValidationException.class);

        verifyNoInteractions(refreshTokenRepository, userRepository, recorder);
    }

    @Test
    void refresh_tokenHashNotFound_throws() {
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash(RAW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refresh(RAW))
                .isInstanceOf(TokenValidationException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(tokenProvider, never()).extractUserId(any());
        verifyNoInteractions(userRepository, recorder);
    }

    @Test
    void refresh_storedTokenNotActive_throws() {
        RefreshToken revoked = new RefreshToken(UUID.randomUUID(), USER_ID, RefreshToken.hash(RAW),
                LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(1), true,
                LocalDateTime.now().minusDays(1));
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash(RAW))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> useCase.refresh(RAW))
                .isInstanceOf(TokenValidationException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(tokenProvider, never()).extractUserId(any());
        verifyNoInteractions(userRepository, recorder);
    }

    @Test
    void refresh_userNotFound_throws() {
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash(RAW))).thenReturn(Optional.of(activeStored()));
        when(tokenProvider.extractUserId(RAW)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refresh(RAW))
                .isInstanceOf(UserNotFoundException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(recorder);
    }

    @Test
    void refresh_lockedUser_throwsBeforeRotation() {
        User locked = new User(USER_ID, EMAIL, "user", HASH, true, true, true, 0,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().minusDays(1), null, null);
        when(tokenProvider.validateRefreshToken(RAW)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash(RAW))).thenReturn(Optional.of(activeStored()));
        when(tokenProvider.extractUserId(RAW)).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> useCase.refresh(RAW))
                .isInstanceOf(AccountLockedException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(recorder);
    }
}
