package ru.eunoia.application.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {

    // === CRUD операции ===
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findById(UUID id);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteById(UUID id);
    void delete(RefreshToken token);

    // === Поиск по пользователю ===
    List<RefreshToken> findByUserId(UUID userId);
    List<RefreshToken> findByUserIdAndRevoked(UUID userId, boolean revoked);

    // === Проверки ===
    boolean existsByTokenHash(String tokenHash);
    boolean existsByUserIdAndRevoked(UUID userId, boolean revoked);
    long countByUserId(UUID userId);

    // === Бизнес-операции ===
    void revokeByUserId(UUID userId);
    void revokeByTokenHash(String tokenHash);
    void revokeAllExpiredTokens();
    void deleteAllByUserId(UUID userId);

    // === Пакетные операции ===
    List<RefreshToken> findAllExpiredTokens();
    List<RefreshToken> findAllRevokedTokens();

}
