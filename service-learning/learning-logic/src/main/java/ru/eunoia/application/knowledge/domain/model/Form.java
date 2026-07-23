package ru.eunoia.application.knowledge.domain.model;

/** Словоформа: текст + грамматический признак (past, plural, 3sg…). Пример: go → (went, "past"). */
public record Form(String text, String feature) {
}
