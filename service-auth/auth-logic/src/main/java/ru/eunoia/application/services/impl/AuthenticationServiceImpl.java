package ru.eunoia.application.services.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;
import ru.eunoia.application.port.out.EmailVerificationRepositoryPort;
import ru.eunoia.application.port.out.PasswordResetRepositoryPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.UserServicePort;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.ServiceUnavailableException;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.AuthEvent.EventType;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.EmailVerificationToken;
import ru.eunoia.application.domain.model.PasswordResetToken;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.services.AuthenticationService;
import org.springframework.transaction.annotation.Transactional;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.TokenProviderPort;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserServicePort userServicePort;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;
    private final PasswordResetRepositoryPort passwordResetRepositoryPort;
    private final AuthEventRepositoryPort authEventRepositoryPort;

    @Override
    @Transactional
    @CircuitBreaker(name = "userService", fallbackMethod = "registerFallback")
    @Retry(name = "userService", fallbackMethod = "registerFallback")
    @TimeLimiter(name = "userService", fallbackMethod = "registerFallback")
    public CompletableFuture<AuthTokens> register(
            String email, String password, String username,
            String firstName, String lastName) {

        return CompletableFuture.supplyAsync(() -> {
            // 1. Проверка существования пользователя
            if (userServicePort.userExistsByEmail(email).join()) {
                throw new UserAlreadyExistsException("User with email " + email + " already exists");
            }
            if (userServicePort.userExistsByUsername(username).join()) {
                throw new UserAlreadyExistsException("User with username " + username + " already exists");
            }

            // 2. Хеширование пароля
            String passwordHash = passwordEncoderPort.encode(password);

            // 3. Создание пользователя
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .username(username)
                    .passwordHash(passwordHash)
                    .firstName(firstName)
                    .lastName(lastName)
                    .emailVerified(false)
                    .active(true)
                    .locked(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 4. Сохранение в user-service
            User savedUser = userServicePort.createUser(user).join();

            // 5. Создание токена верификации email
            createEmailVerificationToken(savedUser);

            // 6. Генерация JWT токенов
            AuthTokens authTokens = tokenProviderPort.generateTokens(savedUser);

            // 7. Сохранение refresh токена
            saveRefreshToken(authTokens.getRefreshToken(), savedUser.getId());

            // 8. Логирование события
            saveAuthEvent(savedUser.getId(), AuthEvent.EventType.LOGIN_SUCCESS, true);

            return authTokens;
        });
    }

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "loginFallback")
    @Retry(name = "userService", fallbackMethod = "loginFallback")
    @TimeLimiter(name = "userService", fallbackMethod = "loginFallback")
    public CompletableFuture<AuthTokens> login(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Поиск пользователя
            User user = userServicePort.findUserByEmail(email)
                    .join()
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            // 2. Проверка пароля
            boolean passwordValid = userServicePort.validatePassword(user.getId(), password).join();
            if (!passwordValid) {
                saveAuthEvent(user.getId(), AuthEvent.EventType.LOGIN_FAILED, false);
                throw new InvalidCredentialsException("Invalid email or password");
            }

            // 3. Проверка статуса аккаунта
            if (!user.isActive()) {
                throw new AccountNotActiveException("Account is not active");
            }
            if (!user.isLocked()) {
                throw new AccountLockedException("Account is locked");
            }

            // 4. Обновление последнего входа
            user.setLastLoginAt(LocalDateTime.now());
            userServicePort.updateUser(user).join();

            // 5. Генерация токенов
            AuthTokens authTokens = tokenProviderPort.generateTokens(user);

            // 6. Сохранение refresh токена
            saveRefreshToken(authTokens.getRefreshToken(), user.getId());

            // 7. Логирование
            saveAuthEvent(user.getId(), AuthEvent.EventType.LOGIN_SUCCESS, true);

            return authTokens;
        });
    }

    @Override
    public CompletableFuture<AuthTokens> refreshToken(String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Валидация refresh токена
            if (!tokenProviderPort.validateRefreshToken(refreshToken)) {
                throw new TokenValidationException("Invalid refresh token");
            }

            // 2. Извлечение userId из токена
            UUID userId = tokenProviderPort.extractUserId(refreshToken);

            // 3. Поиск пользователя
            User user = userServicePort.findUserById(userId)
                    .join()
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // 4. Проверка статуса аккаунта
            if (!user.isActive()) {
                throw new AccountNotActiveException("Account is not active");
            }

            // 5. Поиск сохранённого refresh токена
            String tokenHash = passwordEncoderPort.encode(refreshToken);
            RefreshToken storedToken = refreshTokenRepositoryPort.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new TokenValidationException("Refresh token not found or revoked"));

            if (storedToken.isRevoked()) {
                throw new TokenValidationException("Refresh token has been revoked");
            }

            if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new TokenValidationException("Refresh token expired");
            }

            // 6. Отметка использования
            storedToken.setUsedAt(LocalDateTime.now());
            refreshTokenRepositoryPort.save(storedToken);

            // 7. Генерация новых токенов
            AuthTokens newTokens = tokenProviderPort.generateTokens(user);

            // 8. Сохранение нового refresh токена
            saveRefreshToken(newTokens.getRefreshToken(), user.getId());

            // 9. Логирование
            saveAuthEvent(user.getId(), AuthEvent.EventType.TOKEN_REFRESH, true);

            return newTokens;
        });
    }

    @Override
    @Transactional
    public CompletableFuture<Void> logout(String refreshToken) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 1. Валидация токена
                if (!tokenProviderPort.validateRefreshToken(refreshToken)) {
                    return;
                }

                // 2. Извлечение userId
                UUID userId = tokenProviderPort.extractUserId(refreshToken);

                // 3. Отзыв всех refresh токенов пользователя
                refreshTokenRepositoryPort.revokeByUserId(userId);

                // 4. Логирование
                saveAuthEvent(userId, AuthEvent.EventType.LOGOUT, true);

            } catch (Exception e) {
                log.warn("Error during logout: {}", e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<User> validateToken(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Валидация токена
            if (!tokenProviderPort.validateAccessToken(accessToken)) {
                throw new TokenValidationException("Invalid access token");
            }

            // 2. Извлечение userId
            UUID userId = tokenProviderPort.extractUserId(accessToken);

            // 3. Поиск пользователя
            return userServicePort.findUserById(userId)
                    .join()
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
        });
    }

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "requestPasswordResetFallback")
    public CompletableFuture<Void> requestPasswordReset(String email) {
        return CompletableFuture.runAsync(() -> {
            // 1. Поиск пользователя
            User user = userServicePort.findUserByEmail(email)
                    .join()
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // 2. Создание токена сброса пароля
            PasswordResetToken resetToken = createPasswordResetToken(user);
            passwordResetRepositoryPort.save(resetToken);

            // 3. Отправка email (здесь должна быть интеграция с email сервисом)
            log.info("Password reset token created for user {}: {}", user.getEmail(), resetToken.getId());

            // TODO: Отправить email с ссылкой для сброса пароля
        });
    }

    @Override
    @Transactional
    public CompletableFuture<Void> resetPassword(String token, String newPassword) {
        return CompletableFuture.runAsync(() -> {
            // 1. Поиск токена
            String tokenHash = passwordEncoderPort.encode(token);
            PasswordResetToken resetToken = passwordResetRepositoryPort.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new TokenValidationException("Invalid or expired token"));

            // 2. Проверка срока действия
            if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new TokenValidationException("Token has expired or already used");
            }

            // 3. Хеширование нового пароля
            String newPasswordHash = passwordEncoderPort.encode(newPassword);

            // 4. Обновление пароля в user-service
            userServicePort.updatePassword(resetToken.getUserId(), newPasswordHash).join();

            // 5. Отметка использования токена
            resetToken.markAsUsed(newPasswordHash);
            passwordResetRepositoryPort.save(resetToken);

            // 6. Отзыв всех активных сессий пользователя
            refreshTokenRepositoryPort.revokeByUserId(resetToken.getUserId());

            // 7. Логирование
            saveAuthEvent(resetToken.getUserId(), AuthEvent.EventType.PASSWORD_CHANGED, true);
        });
    }

    @Override
    @Transactional
    public CompletableFuture<Void> verifyEmail(String token) {
        return CompletableFuture.runAsync(() -> {
            // 1. Поиск токена
            String tokenHash = passwordEncoderPort.encode(token);
            EmailVerificationToken verificationToken = emailVerificationRepositoryPort
                    .findByTokenHash(tokenHash)
                    .orElseThrow(() -> new TokenValidationException("Invalid or expired token"));

            // 2. Проверка срока действия
            if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new TokenValidationException("Token has expired or already used");
            }

            // 3. Поиск пользователя
            User user = userServicePort.findUserById(verificationToken.getUserId())
                    .join()
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // 4. Обновление статуса email
            user.setEmailVerified(true);
            userServicePort.updateUser(user).join();

            // 5. Отметка использования токена
            verificationToken.markAsVerified();
            emailVerificationRepositoryPort.save(verificationToken);

            // 6. Логирование
            saveAuthEvent(user.getId(), EventType.EMAIL_VERIFICATION_SUCCESS, true);
        });
    }

    @Override
    public CompletableFuture<Void> changePassword(String currentPassword, String newPassword, User currentUser) {
        return CompletableFuture.runAsync(() -> {
            // 1. Проверка текущего пароля
            boolean passwordValid = userServicePort
                    .validatePassword(currentUser.getId(), currentPassword)
                    .join();

            if (!passwordValid) {
                throw new InvalidCredentialsException("Current password is incorrect");
            }

            // 2. Хеширование нового пароля
            String newPasswordHash = passwordEncoderPort.encode(newPassword);

            // 3. Обновление пароля
            userServicePort.updatePassword(currentUser.getId(), newPasswordHash).join();

            // 4. Отзыв всех активных сессий (кроме текущей)
            refreshTokenRepositoryPort.revokeByUserId(currentUser.getId());

            // 5. Логирование
            saveAuthEvent(currentUser.getId(), AuthEvent.EventType.PASSWORD_CHANGED, true);
        });
    }

    // =========== Приватные вспомогательные методы ===========

    private void saveRefreshToken(String refreshToken, UUID userId) {
        String tokenHash = passwordEncoderPort.encode(refreshToken);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        refreshTokenRepositoryPort.save(refreshTokenEntity);
    }

    private void createEmailVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        String tokenHash = passwordEncoderPort.encode(token);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .email(user.getEmail())
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        emailVerificationRepositoryPort.save(verificationToken);

        // TODO: Отправить email с ссылкой для верификации
        log.info("Email verification token created for user {}: {}", user.getEmail(), token);
    }

    private PasswordResetToken createPasswordResetToken(User user) {
        String token = UUID.randomUUID().toString();
        String tokenHash = passwordEncoderPort.encode(token);

        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .email(user.getEmail())
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
    }

    private void saveAuthEvent(UUID userId, AuthEvent.EventType eventType, boolean success) {
        AuthEvent event = AuthEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .eventType(eventType)
                .success(success)
                .createdAt(LocalDateTime.now())
                .build();

        authEventRepositoryPort.save(event);
    }

    // =========== Fallback методы для Resilience4j ===========

    private CompletableFuture<AuthTokens> registerFallback(
            String email, String password, String username,
            String firstName, String lastName, Throwable t) {
        log.error("Register fallback triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("User service is unavailable. Please try again later.");
    }

    private CompletableFuture<AuthTokens> loginFallback(String email, String password, Throwable t) {
        log.error("Login fallback triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("User service is unavailable. Please try again later.");
    }

    private CompletableFuture<Void> requestPasswordResetFallback(String email, Throwable t) {
        log.error("Request password reset fallback triggered: {}", t.getMessage());
        // Не бросаем исключение, просто логируем
        return CompletableFuture.completedFuture(null);
    }

}
