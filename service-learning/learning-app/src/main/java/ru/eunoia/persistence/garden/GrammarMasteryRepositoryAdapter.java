package ru.eunoia.persistence.garden;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.garden.port.out.GrammarMasteryRepositoryPort;
import ru.eunoia.persistence.garden.entity.GrammarMasteryEntity;
import ru.eunoia.persistence.garden.entity.GrammarMasteryId;
import ru.eunoia.persistence.garden.repository.GrammarMasteryJpaRepository;

/** JPA-адаптер прогресса по грамматике. Ключ присвоенный (userId+grammarId), не генерим. */
@Component
@RequiredArgsConstructor
public class GrammarMasteryRepositoryAdapter implements GrammarMasteryRepositoryPort {

    private final GrammarMasteryJpaRepository jpaRepository;

    @Override
    public GrammarMastery save(GrammarMastery m) {
        GrammarMasteryEntity entity = new GrammarMasteryEntity();
        entity.setUserId(m.userId());
        entity.setGrammarId(m.grammarId());
        entity.setStatus(m.status().name());
        entity.setUpdatedAt(m.updatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<GrammarMastery> find(UUID userId, String grammarId) {
        return jpaRepository.findById(new GrammarMasteryId(userId, grammarId)).map(this::toDomain);
    }

    @Override
    public List<GrammarMastery> findByUser(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Map<String, MasteryStatus> statusesFor(UUID userId, Collection<String> grammarIds) {
        if (grammarIds.isEmpty()) {
            return Map.of();   // пустой IN не гоняем
        }
        return jpaRepository.findByUserIdAndGrammarIdIn(userId, grammarIds).stream()
                .collect(Collectors.toMap(GrammarMasteryEntity::getGrammarId,
                        e -> MasteryStatus.valueOf(e.getStatus())));
    }

    private GrammarMastery toDomain(GrammarMasteryEntity e) {
        return new GrammarMastery(e.getUserId(), e.getGrammarId(),
                MasteryStatus.valueOf(e.getStatus()), e.getUpdatedAt());
    }
}
