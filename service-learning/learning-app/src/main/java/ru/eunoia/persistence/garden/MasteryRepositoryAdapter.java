package ru.eunoia.persistence.garden;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.out.MasteryRepositoryPort;
import ru.eunoia.persistence.garden.entity.MasteryEntity;
import ru.eunoia.persistence.garden.entity.MasteryId;
import ru.eunoia.persistence.garden.repository.MasteryJpaRepository;

/** JPA-адаптер оверлея мастерства. Ключ присвоенный (userId+lexemeId), не генерим. */
@Component
@RequiredArgsConstructor
public class MasteryRepositoryAdapter implements MasteryRepositoryPort {

    private final MasteryJpaRepository jpaRepository;

    @Override
    public Mastery save(Mastery m) {
        MasteryEntity entity = new MasteryEntity();
        entity.setUserId(m.userId());
        entity.setLexemeId(m.lexemeId());
        entity.setStatus(m.status().name());
        entity.setUpdatedAt(m.updatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Mastery> find(UUID userId, String lexemeId) {
        return jpaRepository.findById(new MasteryId(userId, lexemeId)).map(this::toDomain);
    }

    @Override
    public List<Mastery> findByUser(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Map<String, MasteryStatus> statusesFor(UUID userId, Collection<String> lexemeIds) {
        if (lexemeIds.isEmpty()) {
            return Map.of();   // пустой IN не гоняем
        }
        return jpaRepository.findByUserIdAndLexemeIdIn(userId, lexemeIds).stream()
                .collect(Collectors.toMap(MasteryEntity::getLexemeId,
                        e -> MasteryStatus.valueOf(e.getStatus())));
    }

    private Mastery toDomain(MasteryEntity e) {
        return new Mastery(e.getUserId(), e.getLexemeId(),
                MasteryStatus.valueOf(e.getStatus()), e.getUpdatedAt());
    }
}
