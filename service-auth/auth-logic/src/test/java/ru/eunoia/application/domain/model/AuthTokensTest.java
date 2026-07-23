package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTokensTest {

    @Test
    void bearerConstantHasExpectedValue() {
        assertThat(AuthTokens.BEARER).isEqualTo("Bearer");
    }

    @Test
    void recordAccessorsReturnConstructorValues() {
        UUID userId = UUID.randomUUID();
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime accessExp = issuedAt.plusMinutes(15);
        LocalDateTime refreshExp = issuedAt.plusDays(30);

        AuthTokens tokens = new AuthTokens("access", "refresh", AuthTokens.BEARER,
                900, userId, issuedAt, accessExp, refreshExp);

        assertThat(tokens.accessToken()).isEqualTo("access");
        assertThat(tokens.refreshToken()).isEqualTo("refresh");
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.expiresIn()).isEqualTo(900);
        assertThat(tokens.userId()).isEqualTo(userId);
        assertThat(tokens.issuedAt()).isEqualTo(issuedAt);
        assertThat(tokens.accessTokenExpiresAt()).isEqualTo(accessExp);
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(refreshExp);
    }
}
