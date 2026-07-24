package ru.eunoia.application.knowledge.domain.model;

/**
 * Лёгкая ссылка на слово — ключ леммы + подпись. id вида "en:go" (без части речи в конце):
 * по нему открывается карточка слова. pos — конкретная часть речи ссылки (связи/поиск).
 */
public record WordRef(String id, String lemma, PartOfSpeech pos) {
}
