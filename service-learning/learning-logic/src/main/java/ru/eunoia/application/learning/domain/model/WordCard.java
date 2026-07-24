package ru.eunoia.application.learning.domain.model;

import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.Word;

/**
 * Карточка слова для UI: канон (слово + части речи + связи из Neo4j) + мой статус по лемме
 * (garden из Postgres). Джойн двух графов по ключу леммы — данные остаются раздельными.
 */
public record WordCard(Word word, MasteryStatus status) {
}
