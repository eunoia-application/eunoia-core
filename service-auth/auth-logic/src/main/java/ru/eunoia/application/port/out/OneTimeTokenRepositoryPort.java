package ru.eunoia.application.port.out;

import java.util.Optional;
import ru.eunoia.application.domain.model.OneTimeToken;

/** Хранилище одноразовых токенов (подтверждение email / сброс пароля). */
public interface OneTimeTokenRepositoryPort {

    OneTimeToken save(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);
}
