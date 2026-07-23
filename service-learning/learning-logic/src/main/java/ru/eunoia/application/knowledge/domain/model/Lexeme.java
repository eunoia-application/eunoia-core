package ru.eunoia.application.knowledge.domain.model;

import java.util.List;

/**
 * Слово — единица обучения (лемма + часть речи). «Карточка слова»: сами атрибуты + формы и
 * переводы. Связи (синонимы/темы/грамматика) отдаются отдельными запросами, чтобы не грузить
 * весь граф. На стабильный {@code id} ссылается персональный оверлей мастерства (контекст garden).
 */
public record Lexeme(
        String id,
        String lemma,
        PartOfSpeech pos,
        String lang,
        Cefr cefr,
        Integer freqRank,
        List<Form> forms,
        List<Translation> translations
) {
}
