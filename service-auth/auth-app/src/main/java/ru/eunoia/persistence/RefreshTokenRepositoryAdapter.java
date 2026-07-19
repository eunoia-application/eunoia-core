package ru.eunoia.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.persistence.entity.RefreshTokenEntity;
import ru.eunoia.persistence.repository.RefreshTokenJpaRepository;

/** JPA-адаптер для refresh-токенов. Маппинг domain ↔ entity — inline. */
@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = toEntity(token);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revokeByUserId(UUID userId) {
        jpaRepository.revokeActiveByUserId(userId, LocalDateTime.now());
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
                e.getCreatedAt(), e.getExpiresAt(), e.isRevoked(), e.getRevokedAt());
    }

    private RefreshTokenEntity toEntity(RefreshToken t) {
        return RefreshTokenEntity.builder()
                .id(t.id())
                .userId(t.userId())
                .tokenHash(t.tokenHash())
                .createdAt(t.createdAt())
                .expiresAt(t.expiresAt())
                .revoked(t.revoked())
                .revokedAt(t.revokedAt())
                .build();
    }
}
