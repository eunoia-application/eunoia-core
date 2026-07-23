package ru.eunoia.application.port.out;

import java.util.UUID;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;

/** Выпуск и проверка JWT. Валидируем только refresh — access проверяет resource-server сам. */
public interface TokenProviderPort {

    AuthTokens generateTokens(User user);

    boolean validateRefreshToken(String token);

    UUID extractUserId(String token);
}
