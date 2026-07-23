package ru.eunoia;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.ForgotPasswordRequest;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RefreshTokenRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import com.eunoia.application.auth.model.ResetPasswordRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.eunoia.application.port.out.EmailSenderPort;

/**
 * End-to-end проверка auth на реальном Postgres (Testcontainers).
 * Поднимает весь сервис: Liquibase накатывает схему, register выдаёт токены,
 * повторный login проходит, неверный пароль — 401, JWKS отдаёт публичный ключ,
 * плюс флоу подтверждения почты и сброса пароля. Требует запущенный Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@Testcontainers
@Import(AuthenticationFlowTest.TestEmailConfig.class)
class AuthenticationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    @Autowired
    CapturingEmailSender emailSender;

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

        // logout по access-токену → 204; после него refresh отозванным токеном больше не проходит
        ResponseEntity<Void> loggedOut = client().post().uri("/auth/logout")
                .header("Authorization", "Bearer " + loggedIn.getBody().getAccessToken())
                .retrieve()
                .toBodilessEntity();
        assertThat(loggedOut.getStatusCode().value()).isEqualTo(204);

        HttpStatusCode afterLogout = client().post().uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenRequest(refreshed.getBody().getRefreshToken()))
                .exchange((request, response) -> response.getStatusCode());
        assertThat(afterLogout.value()).isEqualTo(401);
    }

    @Test
    void account_locks_after_too_many_failed_logins() {
        String email = "bob@example.com";
        client().post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, "GoodPass123", "bob_user"))
                .retrieve()
                .toBodilessEntity();

        // 5 неверных попыток: каждая 401, пятая доводит счётчик до лимита и блокирует
        for (int i = 0; i < 5; i++) {
            HttpStatusCode status = client().post().uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(email, "wrong-password"))
                    .exchange((request, response) -> response.getStatusCode());
            assertThat(status.value()).isEqualTo(401);
        }

        // теперь даже верный пароль отклоняется — аккаунт заблокирован (403)
        HttpStatusCode locked = client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "GoodPass123"))
                .exchange((request, response) -> response.getStatusCode());
        assertThat(locked.value()).isEqualTo(403);
    }

    @Test
    void verify_email_then_reset_password() {
        String email = "carol@example.com";
        String password = "InitPass123";

        // регистрация → уходит письмо с токеном подтверждения (ловим заглушкой)
        client().post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, password, "carol_user"))
                .retrieve()
                .toBodilessEntity();
        String verifyToken = emailSender.lastVerificationToken;
        assertThat(verifyToken).isNotBlank();

        // подтверждение почты по токену → 204
        ResponseEntity<Void> verified = client().post().uri("/auth/verify-email?token=" + verifyToken)
                .retrieve()
                .toBodilessEntity();
        assertThat(verified.getStatusCode().value()).isEqualTo(204);

        // запрос сброса → 204 (наличие аккаунта не палим) + уходит письмо с reset-токеном
        ResponseEntity<Void> forgot = client().post().uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ForgotPasswordRequest(email))
                .retrieve()
                .toBodilessEntity();
        assertThat(forgot.getStatusCode().value()).isEqualTo(204);
        String resetToken = emailSender.lastResetToken;
        assertThat(resetToken).isNotBlank();

        // установка нового пароля по reset-токену → 204
        String newPassword = "NewPass456";
        ResponseEntity<Void> reset = client().post().uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResetPasswordRequest(resetToken, newPassword))
                .retrieve()
                .toBodilessEntity();
        assertThat(reset.getStatusCode().value()).isEqualTo(204);

        // старый пароль больше не подходит (401), новый — проходит (200)
        HttpStatusCode oldLogin = client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange((request, response) -> response.getStatusCode());
        assertThat(oldLogin.value()).isEqualTo(401);

        ResponseEntity<AuthResponse> newLogin = client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, newPassword))
                .retrieve()
                .toEntity(AuthResponse.class);
        assertThat(newLogin.getStatusCode().value()).isEqualTo(200);
    }

    /** Заглушка почты для теста: перехватывает отправленные токены вместо реальной отправки. */
    @TestConfiguration
    static class TestEmailConfig {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    static class CapturingEmailSender implements EmailSenderPort {
        volatile String lastVerificationToken;
        volatile String lastResetToken;

        @Override
        public void sendEmailVerification(String email, String token) {
            this.lastVerificationToken = token;
        }

        @Override
        public void sendPasswordReset(String email, String token) {
            this.lastResetToken = token;
        }
    }
}
