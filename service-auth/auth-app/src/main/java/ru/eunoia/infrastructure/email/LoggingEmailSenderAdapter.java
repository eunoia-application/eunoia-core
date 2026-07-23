package ru.eunoia.infrastructure.email;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.out.EmailSenderPort;

/** Дев-заглушка: печатает письмо в лог (видно токен для перехода по ссылке). В проде — реальный SMTP. */
@Log4j2
@Component
public class LoggingEmailSenderAdapter implements EmailSenderPort {

    @Override
    public void sendEmailVerification(String email, String token) {
        log.info("[email] подтверждение почты {} — токен: {}", email, token);
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        log.info("[email] сброс пароля {} — токен: {}", email, token);
    }
}
