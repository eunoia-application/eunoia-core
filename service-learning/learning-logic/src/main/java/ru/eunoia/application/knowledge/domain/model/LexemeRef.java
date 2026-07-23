package ru.eunoia.application.knowledge.domain.model;

/**
 * Лёгкая ссылка на слово — id + подпись, без форм/переводов. Отдаём в списках (связи,
 * поиск, слова темы), чтобы не тянуть полный граф.
 */
public record LexemeRef(String id, String lemma, PartOfSpeech pos) {
}
