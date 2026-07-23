package ru.eunoia.persistence;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.persistence.entity.OneTimeTokenEntity;
import ru.eunoia.persistence.repository.OneTimeTokenJpaRepository;

/** JPA-адаптер одноразовых токенов. id руками не ставим — генерит @GeneratedValue при insert. */
@Component
@RequiredArgsConstructor
public class OneTimeTokenRepositoryAdapter implements OneTimeTokenRepositoryPort {

    private final OneTimeTokenJpaRepository jpaRepository;

    @Override
    public OneTimeToken save(OneTimeToken token) {
        return toDomain(jpaRepository.save(toEntity(token)));
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    private OneTimeTokenEntity toEntity(OneTimeToken t) {
        OneTimeTokenEntity e = new OneTimeTokenEntity();
        e.setId(t.id()); // null у нового (insert); заполнен у asUsed() (update той же строки)
        e.setUserId(t.userId());
        e.setPurpose(t.purpose().name());
        e.setTokenHash(t.tokenHash());
        e.setCreatedAt(t.createdAt());
        e.setExpiresAt(t.expiresAt());
        e.setUsed(t.used());
        e.setUsedAt(t.usedAt());
        return e;
    }

    private OneTimeToken toDomain(OneTimeTokenEntity e) {
        return new OneTimeToken(e.getId(), e.getUserId(),
                OneTimeToken.Purpose.valueOf(e.getPurpose()), e.getTokenHash(),
                e.getCreatedAt(), e.getExpiresAt(), e.isUsed(), e.getUsedAt());
    }
}
