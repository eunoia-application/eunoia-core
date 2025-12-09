package ru.eunoia.application.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.domain.model.EmailVerificationToken;

public interface EmailVerificationRepositoryPort {

    // === CRUD операции ===
    EmailVerificationToken save(EmailVerificationToken token);
    Optional<EmailVerificationToken> findById(UUID id);
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteById(UUID id);

    // === Поиск ===
    Optional<EmailVerificationToken> findByUserId(UUID userId);
    Optional<EmailVerificationToken> findByEmail(String email);
    List<EmailVerificationToken> findAllByUserId(UUID userId);

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

}
