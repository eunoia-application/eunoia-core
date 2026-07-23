package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {

    public enum EventType {
        // Аутентификация
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_VALIDATED,
        TOKEN_INVALID,

        // Регистрация и активация
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILED,
        EMAIL_VERIFICATION_SENT,
        EMAIL_VERIFICATION_SUCCESS,
        EMAIL_VERIFICATION_FAILED,

        // Пароли
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_SUCCESS,
        PASSWORD_RESET_FAILED,

        // Аккаунт
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        ACCOUNT_ACTIVATED,
        ACCOUNT_DEACTIVATED,

        // Безопасность
        SUSPICIOUS_ACTIVITY,
        BRUTE_FORCE_ATTEMPT,

        // Системные
        SERVICE_UNAVAILABLE,
        EXTERNAL_SERVICE_ERROR
    }

    private UUID id;
    private UUID userId;
    private EventType eventType;

    // Контекст события
    private String ipAddress;
    private String userAgent;
    private String deviceId;
    private String sessionId;
    private String location; // Можно определить по IP

    // Детали
    private String details; // JSON с дополнительной информацией
    private boolean success;
    private String errorMessage;
    private String errorCode;

    // Метрики
    private Long responseTimeMs; // Время обработки запроса
    private String resource; // Ресурс (/auth/login, /auth/register и т.д.)
    private String httpMethod; // GET, POST и т.д.

    // Метаданные
    private String correlationId; // Для трассировки
    private String serviceVersion;
    private String environment;

    private LocalDateTime createdAt;
}
