package ru.eunoia.application.knowledge.domain.model;

import java.util.List;

/**
 * Лёгкая сводка слова для списков (блок, тема, все слова): ключ леммы + части речи +
 * представительный уровень/частота + темы (категории). Без форм/переводов/связей — их только в карточке.
 */
public record WordSummary(
        String id,
        String lemma,
        List<PartOfSpeech> pos,
        Cefr cefr,
        Integer freqRank,
        List<Topic> topics
) {
}
