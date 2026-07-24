package ru.eunoia.application.knowledge.domain.model;

import java.util.List;

/**
 * Слово — единица обучения (лемма). Части речи собраны в {@code variants}. id — ключ леммы
 * "en:go" (без POS): на него ссылается персональный оверлей мастерства (по лемме, garden).
 * freqRank — лучшая (минимальная) частота среди вариантов; ipa — общая транскрипция.
 */
public record Word(
        String id,
        String lemma,
        String ipa,
        Integer freqRank,
        List<LexemeVariant> variants
) {
}
