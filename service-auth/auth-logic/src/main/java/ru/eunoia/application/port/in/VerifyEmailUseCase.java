package ru.eunoia.application.port.in;

/** Подтверждение email по одноразовому токену из письма. */
public interface VerifyEmailUseCase {

    void verify(String token);
}
