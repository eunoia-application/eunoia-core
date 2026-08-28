package ru.eunoia.application.garden.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;

/** Хранилище прогресса по грамматике (Postgres, таблица grammar_mastery). Реализуется JPA в learning-app. */
public interface GrammarMasteryRepositoryPort {

    GrammarMastery save(GrammarMastery mastery);

    Optional<GrammarMastery> find(UUID userId, String grammarId);

    List<GrammarMastery> findByUser(UUID userId);

    /** Статусы по набору правил — для оверлея ствола грамматики: id правила → статус. */
    Map<String, MasteryStatus> statusesFor(UUID userId, Collection<String> grammarIds);
}
