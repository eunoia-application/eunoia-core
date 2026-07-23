package ru.eunoia.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.domain.model.Avatar;

/**
 * Хранилище байтов аватара. Сейчас реализовано Postgres bytea; порт даёт заменить
 * на S3/MinIO без правки ядра — меняется только адаптер в user-app.
 */
public interface AvatarStoragePort {

    void store(UUID userId, Avatar avatar);

    Optional<Avatar> load(UUID userId);

    void delete(UUID userId);
}
