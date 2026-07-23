package ru.eunoia.application.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;

/**
 * Auth-view пользователя: только учётные данные и состояние безопасности.
 * Профиль (имя, аватар, настройки) — в service-user (Design B), не здесь.
 */
public record User(
        UUID id,
        String email,
        String username,
        String passwordHash,
        boolean emailVerified,
        boolean active,
        boolean locked,
        int failedLoginAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {

    /** Свежая регистрация: активен, не подтверждён, не заблокирован. */
    public static User newlyRegistered(String email, String username, String passwordHash) {
        return new User(null, email, username, passwordHash,
                false, true, false, 0, null, null, null, null);
    }

    /** Доменное правило: можно ли сейчас пройти аутентификацию? Бросает, если нет. */
    public void assertCanAuthenticate() {
        if (!active) {
            throw new AccountNotActiveException();
        }
        if (isCurrentlyLocked()) {
            throw new AccountLockedException();
        }
    }

    /** Заблокирован и срок блокировки ещё не истёк. */
    public boolean isCurrentlyLocked() {
        return locked && lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /** Ещё одна неудачная попытка входа; при достижении лимита — блокируем на lockDuration. */
    public User withFailedLoginAttempt(int maxAttempts, Duration lockDuration) {
        int attempts = failedLoginAttempts + 1;
        boolean lock = attempts >= maxAttempts;
        LocalDateTime until = lock ? LocalDateTime.now().plus(lockDuration) : lockedUntil;
        return new User(id, email, username, passwordHash, emailVerified, active,
                lock || locked, attempts, until, createdAt, updatedAt, lastLoginAt);
    }

    /** Успешный вход: сбрасываем счётчик и блокировку, обновляем время последнего входа. */
    public User withSuccessfulLogin() {
        return new User(id, email, username, passwordHash, emailVerified, active,
                false, 0, null, createdAt, updatedAt, LocalDateTime.now());
    }

    /** Почта подтверждена. */
    public User withEmailVerified() {
        return new User(id, email, username, passwordHash, true, active,
                locked, failedLoginAttempts, lockedUntil, createdAt, LocalDateTime.now(), lastLoginAt);
    }

    /** Новый пароль (после сброса). */
    public User withPasswordHash(String newPasswordHash) {
        return new User(id, email, username, newPasswordHash, emailVerified, active,
                locked, failedLoginAttempts, lockedUntil, createdAt, LocalDateTime.now(), lastLoginAt);
    }
}
