package ru.eunoia.application.port.out;

import java.util.Optional;
import ru.eunoia.application.domain.model.User;

/**
 * Outbound port for the auth-owned user store (Design B: service-auth owns credentials).
 * Backed by a JPA adapter in auth-app; replaces the old remote UserServicePort.
 */
public interface UserRepositoryPort {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);
}
