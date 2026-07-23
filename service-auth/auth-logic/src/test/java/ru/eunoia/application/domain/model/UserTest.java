package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;

class UserTest {

    private static final UUID ID = UUID.randomUUID();

    /** Active, verified/unverified-agnostic user with the given security state. */
    private static User user(boolean active, boolean locked, int attempts, LocalDateTime lockedUntil) {
        return new User(ID, "user@eunoia.ru", "user", "hash",
                false, active, locked, attempts, lockedUntil,
                LocalDateTime.now().minusDays(1), null, null);
    }

    @Test
    void newlyRegistered_isActiveUnverifiedUnlocked() {
        User u = User.newlyRegistered("a@b.ru", "alice", "pw-hash");

        assertThat(u.id()).isNull();
        assertThat(u.email()).isEqualTo("a@b.ru");
        assertThat(u.username()).isEqualTo("alice");
        assertThat(u.passwordHash()).isEqualTo("pw-hash");
        assertThat(u.emailVerified()).isFalse();
        assertThat(u.active()).isTrue();
        assertThat(u.locked()).isFalse();
        assertThat(u.failedLoginAttempts()).isZero();
        assertThat(u.lockedUntil()).isNull();
        assertThat(u.createdAt()).isNull();
        assertThat(u.updatedAt()).isNull();
        assertThat(u.lastLoginAt()).isNull();
    }

    @Test
    void assertCanAuthenticate_passesForActiveUnlockedUser() {
        User u = user(true, false, 0, null);

        assertThatCode(u::assertCanAuthenticate).doesNotThrowAnyException();
    }

    @Test
    void assertCanAuthenticate_throwsWhenNotActive() {
        User u = user(false, false, 0, null);

        assertThatThrownBy(u::assertCanAuthenticate)
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void assertCanAuthenticate_throwsWhenCurrentlyLocked() {
        User u = user(true, true, 0, LocalDateTime.now().plusMinutes(30));

        assertThatThrownBy(u::assertCanAuthenticate)
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void isCurrentlyLocked_falseWhenNotLocked() {
        assertThat(user(true, false, 0, LocalDateTime.now().plusMinutes(30)).isCurrentlyLocked())
                .isFalse();
    }

    @Test
    void isCurrentlyLocked_falseWhenLockedButUntilIsNull() {
        assertThat(user(true, true, 0, null).isCurrentlyLocked()).isFalse();
    }

    @Test
    void isCurrentlyLocked_falseWhenLockExpired() {
        assertThat(user(true, true, 0, LocalDateTime.now().minusMinutes(1)).isCurrentlyLocked())
                .isFalse();
    }

    @Test
    void isCurrentlyLocked_trueWhenLockedAndUntilInFuture() {
        assertThat(user(true, true, 0, LocalDateTime.now().plusMinutes(1)).isCurrentlyLocked())
                .isTrue();
    }

    @Test
    void withFailedLoginAttempt_belowLimit_incrementsWithoutLocking() {
        User u = user(true, false, 1, null);

        User r = u.withFailedLoginAttempt(3, Duration.ofMinutes(15));

        assertThat(r).isNotSameAs(u);
        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.failedLoginAttempts()).isEqualTo(2);
        assertThat(r.locked()).isFalse();
        assertThat(r.lockedUntil()).isNull();
    }

    @Test
    void withFailedLoginAttempt_reachingLimit_locksAndSetsLockedUntil() {
        User u = user(true, false, 2, null);

        LocalDateTime before = LocalDateTime.now();
        User r = u.withFailedLoginAttempt(3, Duration.ofMinutes(15));
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(u);
        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.failedLoginAttempts()).isEqualTo(3);
        assertThat(r.locked()).isTrue();
        assertThat(r.lockedUntil()).isBetween(before.plusMinutes(15), after.plusMinutes(15));
    }

    @Test
    void withFailedLoginAttempt_belowLimitButAlreadyLocked_staysLockedKeepingUntil() {
        LocalDateTime existingUntil = LocalDateTime.now().plusMinutes(5);
        User u = user(true, true, 0, existingUntil);

        User r = u.withFailedLoginAttempt(3, Duration.ofMinutes(15));

        assertThat(r.failedLoginAttempts()).isEqualTo(1);
        assertThat(r.locked()).isTrue();
        assertThat(r.lockedUntil()).isEqualTo(existingUntil);
    }

    @Test
    void withSuccessfulLogin_resetsAttemptsAndLockAndStampsLastLogin() {
        User u = user(true, true, 3, LocalDateTime.now().plusMinutes(30));

        LocalDateTime before = LocalDateTime.now();
        User r = u.withSuccessfulLogin();
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(u);
        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.failedLoginAttempts()).isZero();
        assertThat(r.locked()).isFalse();
        assertThat(r.lockedUntil()).isNull();
        assertThat(r.lastLoginAt()).isBetween(before, after);
    }

    @Test
    void withEmailVerified_setsFlagAndStampsUpdatedAt() {
        User u = user(true, false, 0, null);

        LocalDateTime before = LocalDateTime.now();
        User r = u.withEmailVerified();
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(u);
        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.emailVerified()).isTrue();
        assertThat(r.updatedAt()).isBetween(before, after);
    }

    @Test
    void withPasswordHash_replacesHashAndStampsUpdatedAt() {
        User u = user(true, false, 0, null);

        LocalDateTime before = LocalDateTime.now();
        User r = u.withPasswordHash("new-hash");
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(u);
        assertThat(r.id()).isEqualTo(ID);
        assertThat(r.passwordHash()).isEqualTo("new-hash");
        assertThat(r.updatedAt()).isBetween(before, after);
    }
}
