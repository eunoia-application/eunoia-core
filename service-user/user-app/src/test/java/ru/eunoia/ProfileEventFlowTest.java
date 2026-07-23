package ru.eunoia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eunoia.application.user.model.UserProfile;
import com.eunoia.application.user.model.UserPublicProfile;
import com.eunoia.application.user.model.UserSettings;
import com.eunoia.application.user.model.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ru.eunoia.messaging.event.UserDeletedEvent;
import ru.eunoia.messaging.event.UserRegisteredEvent;

/**
 * End-to-end проверка событийного флоу профиля на реальных Postgres + Kafka (Testcontainers).
 * auth не поднимаем: UserRegistered кладём в топик руками, JWT подменяем тестовым декодером
 * (bearer = userId). Проверяем: событие создаёт профиль, REST его отдаёт, обновление и публичность
 * работают, а UserDeleted профиль удаляет. Требует запущенный Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
        })
@Testcontainers
@Import(ProfileEventFlowTest.TestSecurityConfig.class)
class ProfileEventFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @Value("${local.server.port}")
    int port;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void userRegistered_creates_profile_and_userDeleted_removes_it() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "alex@example.com";
        String username = "alex_user";

        // auth опубликовал UserRegistered → консюмер должен создать профиль
        publish("user.registered", userId, new UserRegisteredEvent(userId, email, username, LocalDateTime.now()));

        // ждём, пока профиль появится: GET /users/me (bearer = userId) отдаёт 200
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(meStatus(userId).value()).isEqualTo(200));

        UserProfile profile = client().get().uri("/users/me")
                .header("Authorization", "Bearer " + userId)
                .retrieve()
                .body(UserProfile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo(email);
        assertThat(profile.getUsername()).isEqualTo(username);

        // обновляем профиль → 200 + новые поля
        UserUpdateRequest update = new UserUpdateRequest();
        update.setFirstName("Alex");
        update.setLastName("Johnson");
        update.setBio("Изучаю английский");
        UserProfile updated = client().put().uri("/users/me")
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve()
                .body(UserProfile.class);
        assertThat(updated.getFirstName()).isEqualTo("Alex");
        assertThat(updated.getLastName()).isEqualTo("Johnson");

        // по умолчанию профиль приватный → публичный GET отдаёт 403
        HttpStatusCode whenPrivate = client().get().uri("/users/" + userId)
                .exchange((req, res) -> res.getStatusCode());
        assertThat(whenPrivate.value()).isEqualTo(403);

        // делаем профиль публичным через настройки
        UserSettings settings = new UserSettings();
        settings.setTheme(UserSettings.ThemeEnum.DARK);
        settings.setProfileVisibility(UserSettings.ProfileVisibilityEnum.PUBLIC);
        settings.setInterfaceLanguage("ru");
        settings.setEmailNotifications(true);
        settings.setAiSuggestionsEnabled(true);
        client().put().uri("/users/me/settings")
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(settings)
                .retrieve()
                .toBodilessEntity();

        // теперь публичный профиль виден без токена (200)
        UserPublicProfile pub = client().get().uri("/users/" + userId)
                .retrieve()
                .body(UserPublicProfile.class);
        assertThat(pub).isNotNull();
        assertThat(pub.getUsername()).isEqualTo(username);

        // auth опубликовал UserDeleted → профиль исчезает, GET /users/me отдаёт 404
        publish("user.deleted", userId, new UserDeletedEvent(userId, LocalDateTime.now()));
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(meStatus(userId).value()).isEqualTo(404));
    }

    /** Кладём событие как JSON-строку (консюмер читает String + StringJsonMessageConverter). */
    private void publish(String topic, UUID userId, Object event) throws Exception {
        kafkaTemplate.send(topic, userId.toString(), objectMapper.writeValueAsString(event));
    }

    private HttpStatusCode meStatus(UUID userId) {
        return client().get().uri("/users/me")
                .header("Authorization", "Bearer " + userId)
                .exchange((req, res) -> res.getStatusCode());
    }

    /** Тестовый декодер: bearer-токен = userId (auth не поднимаем, подпись не проверяем). */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("userId", token)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
        }
    }
}
