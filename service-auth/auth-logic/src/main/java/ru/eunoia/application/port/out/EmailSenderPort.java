package ru.eunoia.application.port.out;

/** Отправка писем auth-флоу. В деве — заглушка в лог, в проде — реальный SMTP. */
public interface EmailSenderPort {

    void sendEmailVerification(String email, String token);

    void sendPasswordReset(String email, String token);
}
