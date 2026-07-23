package ru.eunoia.application.port.in;

import ru.eunoia.application.domain.model.Authentication;

public interface RefreshTokenUseCase {

    Authentication refresh(String refreshToken);
}
