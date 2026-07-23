package ru.eunoia.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.User;

/**
 * Чистое преобразование DTO контракта ↔ домен. Без моков — конструируем реальные объекты.
 */
class AuthApiMapperTest {

    private final AuthApiMapper mapper = new AuthApiMapper();

    @Test
    void toCommand_login_copiesEmailAndPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "secret");

        LoginCommand command = mapper.toCommand(request);

        assertThat(command.email()).isEqualTo("user@example.com");
        assertThat(command.password()).isEqualTo("secret");
    }

    @Test
    void toCommand_register_copiesEmailPasswordAndUsername() {
        RegisterRequest request = new RegisterRequest("user@example.com", "secret12", "eunoia_user");

        RegisterCommand command = mapper.toCommand(request);

        assertThat(command.email()).isEqualTo("user@example.com");
        assertThat(command.password()).isEqualTo("secret12");
        assertThat(command.username()).isEqualTo("eunoia_user");
    }

    @Test
    void toResponse_mapsTokensAndFullUserProfile() {
        UUID userId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        LocalDateTime updated = LocalDateTime.of(2026, 6, 7, 8, 9, 10);
        User user = new User(
                userId, "user@example.com", "eunoia_user", "password-hash",
                true, true, false, 0, null, created, updated, null);
        AuthTokens tokens = new AuthTokens(
                "access-tok", "refresh-tok", AuthTokens.BEARER, 3600,
                userId, created, created.plusHours(1), created.plusDays(7));
        Authentication authentication = new Authentication(user, tokens);

        AuthResponse response = mapper.toResponse(authentication);

        assertThat(response.getAccessToken()).isEqualTo("access-tok");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-tok");
        assertThat(response.getTokenType()).isEqualTo(AuthResponse.TokenTypeEnum.BEARER);
        assertThat(response.getExpiresIn()).isEqualTo(3600);

        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getUser().getUsername()).isEqualTo("eunoia_user");
        assertThat(response.getUser().getEmailVerified()).isTrue();
        assertThat(response.getUser().getCreatedAt()).isEqualTo(created);
        assertThat(response.getUser().getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toResponse_mapsUnverifiedEmailAndNullUpdatedAt() {
        UUID userId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 3, 3, 3, 3, 3);
        User user = new User(
                userId, "fresh@example.com", "fresh_user", "password-hash",
                false, true, false, 0, null, created, null, null);
        AuthTokens tokens = new AuthTokens(
                "a", "r", AuthTokens.BEARER, 60,
                userId, created, created, created);

        AuthResponse response = mapper.toResponse(new Authentication(user, tokens));

        assertThat(response.getUser().getEmailVerified()).isFalse();
        assertThat(response.getUser().getUpdatedAt()).isNull();
    }
}
