package ru.eunoia.application.learning.domain.model;

import java.util.List;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.Cefr;
import ru.eunoia.application.knowledge.domain.model.PartOfSpeech;
import ru.eunoia.application.knowledge.domain.model.Topic;

/**
 * Слово-лист (в блоке, теме или списке всех слов) с моим статусом — из этого фронт красит сад.
 * id — ключ леммы "en:go"; pos — части речи слова; topics — категории (для разбивки внутри блока).
 */
public record WordLeaf(
        String id,
        String lemma,
        List<PartOfSpeech> pos,
        Cefr cefr,
        List<Topic> topics,
        MasteryStatus status
) {
}
