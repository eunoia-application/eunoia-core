package ru.eunoia.application.knowledge.domain.model;

import java.util.List;

/**
 * Грамматическое правило — «ствол». Слова его иллюстрируют (ILLUSTRATES), правила упорядочены
 * (PREREQUISITE): {@code prerequisites} — id правил, которые желательно знать раньше.
 */
public record Grammar(String id, String name, Cefr cefr, List<String> prerequisites) {
}
