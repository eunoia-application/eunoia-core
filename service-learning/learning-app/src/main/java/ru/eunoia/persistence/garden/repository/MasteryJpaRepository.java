package ru.eunoia.persistence.garden.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.eunoia.persistence.garden.entity.MasteryEntity;
import ru.eunoia.persistence.garden.entity.MasteryId;

public interface MasteryJpaRepository extends JpaRepository<MasteryEntity, MasteryId> {

    List<MasteryEntity> findByUserId(UUID userId);

    List<MasteryEntity> findByUserIdAndLexemeIdIn(UUID userId, Collection<String> lexemeIds);
}
