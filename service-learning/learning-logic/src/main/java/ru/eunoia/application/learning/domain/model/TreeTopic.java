package ru.eunoia.application.learning.domain.model;

/** Ветка сада: тема + мой прогресс по ней (для роста/плотности ветки на дереве). */
public record TreeTopic(String id, String name, String slug, int known, int learning, int total) {
}
