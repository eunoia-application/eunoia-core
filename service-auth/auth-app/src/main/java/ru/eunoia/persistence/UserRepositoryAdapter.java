package ru.eunoia.persistence;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.UserRepositoryPort;
import ru.eunoia.persistence.entity.UserEntity;
import ru.eunoia.persistence.repository.UserJpaRepository;

/**
 * JPA-backed outbound adapter for {@link UserRepositoryPort}. Auth owns the user store
 * (Design B). Domain ↔ entity mapping is inline (no MapStruct) to stay runtime-safe.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return toDomain(jpaRepository.save(entity));
    }

    private User toDomain(UserEntity e) {
        return new User(
                e.getId(),
                e.getEmail(),
                e.getUsername(),
                e.getPasswordHash(),
                e.isEmailVerified(),
                e.isActive(),
                e.isLocked(),
                e.getFailedLoginAttempts(),
                e.getLockedUntil(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getLastLoginAt()
        );
    }

    private UserEntity toEntity(User u) {
        return UserEntity.builder()
                .id(u.id())
                .email(u.email())
                .username(u.username())
                .passwordHash(u.passwordHash())
                .emailVerified(u.emailVerified())
                .active(u.active())
                .locked(u.locked())
                .failedLoginAttempts(u.failedLoginAttempts())
                .lockedUntil(u.lockedUntil())
                .createdAt(u.createdAt())
                .updatedAt(u.updatedAt())
                .lastLoginAt(u.lastLoginAt())
                .build();
    }
}
