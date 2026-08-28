package ru.eunoia.persistence.garden.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.eunoia.persistence.garden.entity.GrammarMasteryEntity;
import ru.eunoia.persistence.garden.entity.GrammarMasteryId;

public interface GrammarMasteryJpaRepository extends JpaRepository<GrammarMasteryEntity, GrammarMasteryId> {

    List<GrammarMasteryEntity> findByUserId(UUID userId);

    List<GrammarMasteryEntity> findByUserIdAndGrammarIdIn(UUID userId, Collection<String> grammarIds);
}
