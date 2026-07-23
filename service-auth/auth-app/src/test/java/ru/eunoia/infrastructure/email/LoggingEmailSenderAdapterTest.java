package ru.eunoia.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * Дев-заглушка почты только пишет в лог. В интеграционном тесте её подменяет фейк (@Primary),
 * поэтому реальный адаптер покрываем здесь напрямую — просто проверяем, что не падает.
 */
class LoggingEmailSenderAdapterTest {

    private final LoggingEmailSenderAdapter sender = new LoggingEmailSenderAdapter();

    @Test
    void sends_verification_without_throwing() {
        assertThatCode(() -> sender.sendEmailVerification("user@example.com", "verify-token"))
                .doesNotThrowAnyException();
    }

    @Test
    void sends_password_reset_without_throwing() {
        assertThatCode(() -> sender.sendPasswordReset("user@example.com", "reset-token"))
                .doesNotThrowAnyException();
    }
}
