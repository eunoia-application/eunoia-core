package ru.eunoia.application.knowledge.domain.model;

/** Перевод слова на язык обучающегося. Пример: go → ("идти", "ru"). */
public record Translation(String text, String lang) {
}
