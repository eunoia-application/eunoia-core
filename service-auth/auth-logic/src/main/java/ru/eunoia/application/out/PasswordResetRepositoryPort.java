package ru.eunoia.application.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.domain.model.PasswordResetToken;

public interface PasswordResetRepositoryPort {

    // === CRUD операции ===
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findById(UUID id);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteById(UUID id);

    // === Поиск ===
    Optional<PasswordResetToken> findByUserId(UUID userId);
    Optional<PasswordResetToken> findByEmail(String email);
    List<PasswordResetToken> findAllByUserId(UUID userId);
    List<PasswordResetToken> findAllUnusedByUserId(UUID userId);

    // === Проверки ===
    boolean existsByUserId(UUID userId);
    boolean existsByEmail(String email);
    boolean existsByTokenHash(String tokenHash);

    // === Бизнес-операции ===
    void deleteByUserId(UUID userId);
    void deleteByEmail(String email);
    void deleteExpiredTokens();
    void markAsUsed(String tokenHash);

    // === Статистика ===
    long countByUserId(UUID userId);
    long countUnusedByUserId(UUID userId);
    long countExpiredTokens();

}
