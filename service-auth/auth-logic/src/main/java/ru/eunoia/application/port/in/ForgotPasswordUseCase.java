package ru.eunoia.application.port.in;

/** Запрос сброса пароля: по email отправляем письмо с одноразовым токеном. */
public interface ForgotPasswordUseCase {

    void requestReset(String email);
}
