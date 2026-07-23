package ru.eunoia.application.port.in;

/** Установка нового пароля по одноразовому токену из письма. */
public interface ResetPasswordUseCase {

    void reset(String token, String newPassword);
}
