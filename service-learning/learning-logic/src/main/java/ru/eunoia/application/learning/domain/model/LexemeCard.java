package ru.eunoia.application.learning.domain.model;

import java.util.List;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;

/**
 * Карточка слова для UI: канон (слово + связи из Neo4j) + мой статус (garden из Postgres).
 * Джойн двух графов по id — сами данные остаются раздельными (принцип двух графов).
 */
public record LexemeCard(
        Lexeme lexeme,
        List<LexemeRef> synonyms,
        List<LexemeRef> antonyms,
        List<LexemeRef> hypernyms,
        MasteryStatus status
) {
}
