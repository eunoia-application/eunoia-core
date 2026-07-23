package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationTest {

    @Test
    void recordAccessorsExposeUserAndTokens() {
        User user = User.newlyRegistered("a@b.ru", "alice", "hash");
        AuthTokens tokens = new AuthTokens("access", "refresh", AuthTokens.BEARER,
                900, UUID.randomUUID(), LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15), LocalDateTime.now().plusDays(30));

        Authentication auth = new Authentication(user, tokens);

        assertThat(auth.user()).isSameAs(user);
        assertThat(auth.tokens()).isSameAs(tokens);
    }
}
