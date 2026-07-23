package ru.eunoia.application.knowledge.domain.model;

/** Грамматическое правило — «ствол». Слова его иллюстрируют (ILLUSTRATES), правила упорядочены (PREREQUISITE). */
public record Grammar(String id, String name, Cefr cefr) {
}
