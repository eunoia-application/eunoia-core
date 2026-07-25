package ru.eunoia.application.learning.domain.model;

/** Листья дерева: суммарный словарный прогресс — сколько знаю/учу из всех слов графа. */
public record TreeVocabulary(int known, int learning, int total) {
}
