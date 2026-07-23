package ru.eunoia.application.knowledge.domain.model;

/** Тема — «ветка сада». Группирует слова; сама может ветвиться (SUBTOPIC в графе). */
public record Topic(String id, String name, String slug) {
}
