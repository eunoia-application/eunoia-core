package ru.eunoia.application.garden.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.eunoia.application.garden.domain.model.Mastery;
import ru.eunoia.application.garden.domain.model.MasteryStatus;

/** Пользователь отмечает владение словами и читает свой прогресс. */
public interface MasteryUseCase {

    /** Отметить статус слова (знаю/учу/не знаю) — upsert. */
    Mastery setStatus(UUID userId, String lexemeId, MasteryStatus status);

    /** Статусы по набору слов (для раскраски сада). */
    Map<String, MasteryStatus> statuses(UUID userId, Collection<String> lexemeIds);

    /** Весь мой прогресс (что отмечено). */
    List<Mastery> myMastery(UUID userId);
}
