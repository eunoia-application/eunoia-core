package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;

/**
 * Auth-view of a user: credentials and security state only.
 * Profile data (name, avatar, settings, stats, ...) belongs to service-user (Design B),
 * not to the auth bounded context.
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

    /** A brand-new registration: active, unverified, unlocked, no failed attempts yet. */
    public static User newlyRegistered(String email, String username, String passwordHash) {
        return new User(null, email, username, passwordHash,
                false, true, false, 0, null, null, null, null);
    }

    /** Domain rule: may this account complete authentication right now? Throws if not. */
    public void assertCanAuthenticate() {
        if (!active) {
            throw new AccountNotActiveException();
        }
        if (locked) {
            throw new AccountLockedException();
        }
    }
}
