package ru.eunoia.application.garden.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.GrammarMastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;

/** Пользователь отмечает владение грамматическими правилами (2 кнопки Знаю/Учу) и читает прогресс. */
public interface GrammarMasteryUseCase {

    /** Отметить статус правила (знаю/учу/не знаю) — upsert; фиксирует день занятий. */
    GrammarMastery setStatus(UUID userId, String grammarId, MasteryStatus status);

    /** Статусы по набору правил (для оверлея ствола грамматики). */
    Map<String, MasteryStatus> statuses(UUID userId, Collection<String> grammarIds);

    /** Весь мой прогресс по грамматике (для высоты дерева). */
    List<GrammarMastery> findByUser(UUID userId);
}
