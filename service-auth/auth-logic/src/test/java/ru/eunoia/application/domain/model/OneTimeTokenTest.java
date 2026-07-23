package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.domain.model.OneTimeToken.Purpose;

class OneTimeTokenTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void issued_storesHashNotRawAndIsUnused() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        LocalDateTime before = LocalDateTime.now();
        OneTimeToken t = OneTimeToken.issued("raw", USER_ID, Purpose.EMAIL_VERIFICATION, expiresAt);
        LocalDateTime after = LocalDateTime.now();

        assertThat(t.id()).isNull();
        assertThat(t.userId()).isEqualTo(USER_ID);
        assertThat(t.purpose()).isEqualTo(Purpose.EMAIL_VERIFICATION);
        assertThat(t.tokenHash()).isEqualTo(OneTimeToken.hash("raw"));
        assertThat(t.tokenHash()).isNotEqualTo("raw");
        assertThat(t.createdAt()).isBetween(before, after);
        assertThat(t.expiresAt()).isEqualTo(expiresAt);
        assertThat(t.used()).isFalse();
        assertThat(t.usedAt()).isNull();
    }

    @Test
    void asUsed_flipsUsedFlagKeepingIdentityAndHash() {
        UUID id = UUID.randomUUID();
        OneTimeToken t = new OneTimeToken(id, USER_ID, Purpose.PASSWORD_RESET, "hash-value",
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1), false, null);

        LocalDateTime before = LocalDateTime.now();
        OneTimeToken used = t.asUsed();
        LocalDateTime after = LocalDateTime.now();

        assertThat(used).isNotSameAs(t);
        assertThat(used.id()).isEqualTo(id);
        assertThat(used.userId()).isEqualTo(USER_ID);
        assertThat(used.purpose()).isEqualTo(Purpose.PASSWORD_RESET);
        assertThat(used.tokenHash()).isEqualTo("hash-value");
        assertThat(used.used()).isTrue();
        assertThat(used.usedAt()).isBetween(before, after);
    }

    @Test
    void isActive_trueWhenNotUsedAndNotExpired() {
        OneTimeToken t = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.EMAIL_VERIFICATION, "h",
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1), false, null);

        assertThat(t.isActive()).isTrue();
    }

    @Test
    void isActive_falseWhenUsed() {
        OneTimeToken t = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.EMAIL_VERIFICATION, "h",
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1), true,
                LocalDateTime.now());

        assertThat(t.isActive()).isFalse();
    }

    @Test
    void isActive_falseWhenExpired() {
        OneTimeToken t = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.EMAIL_VERIFICATION, "h",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), false, null);

        assertThat(t.isActive()).isFalse();
    }

    @Test
    void isFor_trueForMatchingPurposeFalseOtherwise() {
        OneTimeToken t = new OneTimeToken(UUID.randomUUID(), USER_ID, Purpose.PASSWORD_RESET, "h",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false, null);

        assertThat(t.isFor(Purpose.PASSWORD_RESET)).isTrue();
        assertThat(t.isFor(Purpose.EMAIL_VERIFICATION)).isFalse();
    }

    @Test
    void newRawToken_isNonBlankUrlSafeAndUnique() {
        String a = OneTimeToken.newRawToken();
        String b = OneTimeToken.newRawToken();

        assertThat(a).isNotBlank();
        assertThat(a).matches("[A-Za-z0-9_-]+");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hash_isDeterministicAndDiffersForDifferentInput() {
        assertThat(OneTimeToken.hash("abc")).isEqualTo(OneTimeToken.hash("abc"));
        assertThat(OneTimeToken.hash("abc")).isNotEqualTo(OneTimeToken.hash("abd"));
        assertThat(OneTimeToken.hash("abc")).matches("[0-9a-f]{64}");
    }

    @Test
    void hash_throwsIllegalStateWhenInputCannotBeHashed() {
        assertThatThrownBy(() -> OneTimeToken.hash(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void purpose_enumExposesBothValues() {
        assertThat(Purpose.values()).containsExactly(Purpose.EMAIL_VERIFICATION, Purpose.PASSWORD_RESET);
        assertThat(Purpose.valueOf("PASSWORD_RESET")).isEqualTo(Purpose.PASSWORD_RESET);
    }
}
