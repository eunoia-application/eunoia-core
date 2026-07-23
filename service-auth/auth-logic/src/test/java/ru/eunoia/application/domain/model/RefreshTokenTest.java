package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void issued_storesHashNotRawAndIsNotRevoked() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        LocalDateTime before = LocalDateTime.now();
        RefreshToken rt = RefreshToken.issued("raw-token", USER_ID, expiresAt);
        LocalDateTime after = LocalDateTime.now();

        assertThat(rt.id()).isNull();
        assertThat(rt.userId()).isEqualTo(USER_ID);
        assertThat(rt.tokenHash()).isEqualTo(RefreshToken.hash("raw-token"));
        assertThat(rt.tokenHash()).isNotEqualTo("raw-token");
        assertThat(rt.createdAt()).isBetween(before, after);
        assertThat(rt.expiresAt()).isEqualTo(expiresAt);
        assertThat(rt.revoked()).isFalse();
        assertThat(rt.revokedAt()).isNull();
    }

    @Test
    void asRevoked_flipsRevokedFlagKeepingIdentityAndHash() {
        UUID id = UUID.randomUUID();
        RefreshToken rt = new RefreshToken(id, USER_ID, "hash-value",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), false, null);

        LocalDateTime before = LocalDateTime.now();
        RefreshToken revoked = rt.asRevoked();
        LocalDateTime after = LocalDateTime.now();

        assertThat(revoked).isNotSameAs(rt);
        assertThat(revoked.id()).isEqualTo(id);
        assertThat(revoked.userId()).isEqualTo(USER_ID);
        assertThat(revoked.tokenHash()).isEqualTo("hash-value");
        assertThat(revoked.revoked()).isTrue();
        assertThat(revoked.revokedAt()).isBetween(before, after);
    }

    @Test
    void isActive_trueWhenNotRevokedAndNotExpired() {
        RefreshToken rt = new RefreshToken(UUID.randomUUID(), USER_ID, "h",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), false, null);

        assertThat(rt.isActive()).isTrue();
    }

    @Test
    void isActive_falseWhenRevoked() {
        RefreshToken rt = new RefreshToken(UUID.randomUUID(), USER_ID, "h",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true,
                LocalDateTime.now());

        assertThat(rt.isActive()).isFalse();
    }

    @Test
    void isActive_falseWhenExpired() {
        RefreshToken rt = new RefreshToken(UUID.randomUUID(), USER_ID, "h",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), false, null);

        assertThat(rt.isActive()).isFalse();
    }

    @Test
    void hash_isDeterministicAndDiffersForDifferentInput() {
        assertThat(RefreshToken.hash("abc")).isEqualTo(RefreshToken.hash("abc"));
        assertThat(RefreshToken.hash("abc")).isNotEqualTo(RefreshToken.hash("abd"));
        assertThat(RefreshToken.hash("abc")).matches("[0-9a-f]{64}");
    }

    @Test
    void hash_throwsIllegalStateWhenInputCannotBeHashed() {
        assertThatThrownBy(() -> RefreshToken.hash(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
