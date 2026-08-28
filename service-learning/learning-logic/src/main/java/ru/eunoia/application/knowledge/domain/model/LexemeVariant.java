package ru.eunoia.application.knowledge.domain.model;

import java.util.List;

/**
 * Одна часть речи слова: её атрибуты, формы, переводы и связи. Несколько вариантов
 * (VERB/NOUN/…) склеиваются в одно {@link Word} — это и есть «разновидности» на карточке.
 */
public record LexemeVariant(
        PartOfSpeech pos,
        Cefr cefr,
        Integer freqRank,
        List<Form> forms,
        List<Translation> translations,
        List<WordRef> synonyms,
        List<WordRef> antonyms,
        List<WordRef> hypernyms
) {
}
