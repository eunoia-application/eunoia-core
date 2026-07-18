package ru.eunoia.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA persistence model for auth events. Lives in the persistence adapter (auth-app),
 * not in the domain core — the core speaks in {@code AuthEvent}.
 */
@Entity
@Table(name = "auth_events", indexes = {
        @Index(name = "idx_auth_events_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_events_event_type", columnList = "event_type"),
        @Index(name = "idx_auth_events_created_at", columnList = "created_at"),
        @Index(name = "idx_auth_events_success", columnList = "success"),
        @Index(name = "idx_auth_events_correlation_id", columnList = "correlation_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    // Контекст
    @Column(name = "ip_address", length = 45) // IPv6 может быть до 45 символов
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "location", length = 100)
    private String location;

    // Детали
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // Метрики
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "resource", length = 200)
    private String resource;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    // Метаданные
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "service_version", length = 20)
    private String serviceVersion;

    @Column(name = "environment", length = 20)
    private String environment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_VALIDATED,
        TOKEN_INVALID,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILED,
        EMAIL_VERIFICATION_SENT,
        EMAIL_VERIFICATION_SUCCESS,
        EMAIL_VERIFICATION_FAILED,
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_SUCCESS,
        PASSWORD_RESET_FAILED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        ACCOUNT_ACTIVATED,
        ACCOUNT_DEACTIVATED,
        SUSPICIOUS_ACTIVITY,
        BRUTE_FORCE_ATTEMPT,
        SERVICE_UNAVAILABLE,
        EXTERNAL_SERVICE_ERROR
    }
}
