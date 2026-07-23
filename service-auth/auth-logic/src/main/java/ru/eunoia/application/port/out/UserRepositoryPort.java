package ru.eunoia.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.domain.model.User;

/**
 * Outbound-порт для auth-хранилища юзеров (Design B: identity у service-auth).
 * Реализуется JPA-адаптером в auth-app.
 */
public interface UserRepositoryPort {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
