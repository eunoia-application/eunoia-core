package ru.eunoia.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.domain.model.Profile;

/** Хранилище профилей. Ключ — userId (тот же id, что у identity в auth). */
public interface ProfileRepositoryPort {

    Profile save(Profile profile);

    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
