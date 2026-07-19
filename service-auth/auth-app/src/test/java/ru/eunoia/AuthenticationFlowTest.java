package ru.eunoia;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RefreshTokenRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end проверка auth на реальном Postgres (Testcontainers).
 * Поднимает весь сервис: Liquibase накатывает схему, register выдаёт токены,
 * повторный login проходит, неверный пароль — 401, JWKS отдаёт публичный ключ.
 * Требует запущенный Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@Testcontainers
class AuthenticationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void register_then_login_and_reject_wrong_password() {
        String email = "alex@example.com";
        String password = "SecurePass123";

        // регистрация → 201 + токены + профиль с тем же email
        ResponseEntity<AuthResponse> registered = client().post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, password, "alex_user"))
                .retrieve()
                .toEntity(AuthResponse.class);
        assertThat(registered.getStatusCode().value()).isEqualTo(201);
        assertThat(registered.getBody()).isNotNull();
        assertThat(registered.getBody().getAccessToken()).isNotBlank();
        assertThat(registered.getBody().getUser().getEmail()).isEqualTo(email);

        // логин теми же кредами → 200 + токен
        ResponseEntity<AuthResponse> loggedIn = client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .retrieve()
                .toEntity(AuthResponse.class);
        assertThat(loggedIn.getStatusCode().value()).isEqualTo(200);
        assertThat(loggedIn.getBody()).isNotNull();
        assertThat(loggedIn.getBody().getAccessToken()).isNotBlank();

        // refresh по выданному refresh-токену → 200 + новая пара (проверяет ротацию и таблицу)
        ResponseEntity<AuthResponse> refreshed = client().post().uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenRequest(loggedIn.getBody().getRefreshToken()))
                .retrieve()
                .toEntity(AuthResponse.class);
        assertThat(refreshed.getStatusCode().value()).isEqualTo(200);
        assertThat(refreshed.getBody()).isNotNull();
        assertThat(refreshed.getBody().getAccessToken()).isNotBlank();

        // неверный пароль → 401 (exchange — чтобы 4xx не бросал исключение)
        HttpStatusCode wrongStatus = client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "wrong-password"))
                .exchange((request, response) -> response.getStatusCode());
        assertThat(wrongStatus.value()).isEqualTo(401);

        // публичный ключ доступен для проверки RS256-токенов
        ResponseEntity<Map> jwks = client().get().uri("/.well-known/jwks.json")
                .retrieve()
                .toEntity(Map.class);
        assertThat(jwks.getStatusCode().value()).isEqualTo(200);
        assertThat(jwks.getBody()).containsKey("keys");
    }
}
