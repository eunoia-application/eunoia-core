package ru.eunoia.application.garden.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;

/** Хранилище персонального оверлея мастерства (Postgres). Реализуется JPA-адаптером в learning-app. */
public interface MasteryRepositoryPort {

    Mastery save(Mastery mastery);

    Optional<Mastery> find(UUID userId, String lexemeId);

    List<Mastery> findByUser(UUID userId);

    /** Статусы по набору слов — для раскраски сада (garden-view): id слова → статус. */
    Map<String, MasteryStatus> statusesFor(UUID userId, Collection<String> lexemeIds);
}
