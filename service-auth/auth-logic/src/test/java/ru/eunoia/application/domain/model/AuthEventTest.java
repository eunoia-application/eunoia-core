package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.domain.model.AuthEvent.EventType;

class AuthEventTest {

    @Test
    void builder_setsEveryField() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        AuthEvent event = AuthEvent.builder()
                .id(id)
                .userId(userId)
                .eventType(EventType.LOGIN_SUCCESS)
                .ipAddress("127.0.0.1")
                .userAgent("agent")
                .deviceId("device")
                .sessionId("session")
                .location("location")
                .details("details")
                .success(true)
                .errorMessage("error-message")
                .errorCode("error-code")
                .responseTimeMs(42L)
                .resource("/auth/login")
                .httpMethod("POST")
                .correlationId("correlation")
                .serviceVersion("1.0")
                .environment("test")
                .createdAt(createdAt)
                .build();

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType()).isEqualTo(EventType.LOGIN_SUCCESS);
        assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(event.getUserAgent()).isEqualTo("agent");
        assertThat(event.getDeviceId()).isEqualTo("device");
        assertThat(event.getSessionId()).isEqualTo("session");
        assertThat(event.getLocation()).isEqualTo("location");
        assertThat(event.getDetails()).isEqualTo("details");
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorMessage()).isEqualTo("error-message");
        assertThat(event.getErrorCode()).isEqualTo("error-code");
        assertThat(event.getResponseTimeMs()).isEqualTo(42L);
        assertThat(event.getResource()).isEqualTo("/auth/login");
        assertThat(event.getHttpMethod()).isEqualTo("POST");
        assertThat(event.getCorrelationId()).isEqualTo("correlation");
        assertThat(event.getServiceVersion()).isEqualTo("1.0");
        assertThat(event.getEnvironment()).isEqualTo("test");
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void allArgsConstructor_populatesEveryField() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        AuthEvent event = new AuthEvent(id, userId, EventType.TOKEN_REFRESH,
                "ip", "ua", "dev", "sess", "loc", "det", false, "em", "ec",
                7L, "/res", "GET", "corr", "sv", "env", createdAt);

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType()).isEqualTo(EventType.TOKEN_REFRESH);
        assertThat(event.getIpAddress()).isEqualTo("ip");
        assertThat(event.getUserAgent()).isEqualTo("ua");
        assertThat(event.getDeviceId()).isEqualTo("dev");
        assertThat(event.getSessionId()).isEqualTo("sess");
        assertThat(event.getLocation()).isEqualTo("loc");
        assertThat(event.getDetails()).isEqualTo("det");
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("em");
        assertThat(event.getErrorCode()).isEqualTo("ec");
        assertThat(event.getResponseTimeMs()).isEqualTo(7L);
        assertThat(event.getResource()).isEqualTo("/res");
        assertThat(event.getHttpMethod()).isEqualTo("GET");
        assertThat(event.getCorrelationId()).isEqualTo("corr");
        assertThat(event.getServiceVersion()).isEqualTo("sv");
        assertThat(event.getEnvironment()).isEqualTo("env");
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void noArgsConstructorWithSetters_populatesEveryField() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        AuthEvent event = new AuthEvent();
        event.setId(id);
        event.setUserId(userId);
        event.setEventType(EventType.LOGOUT);
        event.setIpAddress("ip");
        event.setUserAgent("ua");
        event.setDeviceId("dev");
        event.setSessionId("sess");
        event.setLocation("loc");
        event.setDetails("det");
        event.setSuccess(true);
        event.setErrorMessage("em");
        event.setErrorCode("ec");
        event.setResponseTimeMs(99L);
        event.setResource("/res");
        event.setHttpMethod("PUT");
        event.setCorrelationId("corr");
        event.setServiceVersion("sv");
        event.setEnvironment("env");
        event.setCreatedAt(createdAt);

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType()).isEqualTo(EventType.LOGOUT);
        assertThat(event.getIpAddress()).isEqualTo("ip");
        assertThat(event.getUserAgent()).isEqualTo("ua");
        assertThat(event.getDeviceId()).isEqualTo("dev");
        assertThat(event.getSessionId()).isEqualTo("sess");
        assertThat(event.getLocation()).isEqualTo("loc");
        assertThat(event.getDetails()).isEqualTo("det");
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorMessage()).isEqualTo("em");
        assertThat(event.getErrorCode()).isEqualTo("ec");
        assertThat(event.getResponseTimeMs()).isEqualTo(99L);
        assertThat(event.getResource()).isEqualTo("/res");
        assertThat(event.getHttpMethod()).isEqualTo("PUT");
        assertThat(event.getCorrelationId()).isEqualTo("corr");
        assertThat(event.getServiceVersion()).isEqualTo("sv");
        assertThat(event.getEnvironment()).isEqualTo("env");
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void eventType_valuesAreRoundTrippableByName() {
        for (EventType type : EventType.values()) {
            assertThat(EventType.valueOf(type.name())).isEqualTo(type);
        }
        assertThat(EventType.values()).contains(
                EventType.LOGIN_SUCCESS,
                EventType.LOGIN_FAILED,
                EventType.LOGOUT,
                EventType.TOKEN_REFRESH,
                EventType.REGISTRATION_SUCCESS,
                EventType.EMAIL_VERIFICATION_SENT,
                EventType.EMAIL_VERIFICATION_SUCCESS,
                EventType.PASSWORD_RESET_REQUESTED,
                EventType.PASSWORD_RESET_SUCCESS);
    }
}
