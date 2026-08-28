package ru.eunoia.application.learning.domain.model;

/** Ствол дерева: прогресс по грамматике — из него фронт считает высоту. */
public record TreeGrammar(int known, int learning, int total) {
}
