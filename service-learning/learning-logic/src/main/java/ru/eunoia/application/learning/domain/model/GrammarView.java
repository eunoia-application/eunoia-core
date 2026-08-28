package ru.eunoia.application.learning.domain.model;

import java.util.List;
import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.WordRef;

/**
 * Вид грамматического правила для API: само правило (с порядком PREREQUISITE) + мой статус владения
 * (2 кнопки Знаю/Учу) + слова-примеры, которые его иллюстрируют (ILLUSTRATES). В списке ствола
 * {@code illustratedBy} пуст (не грузим слова на каждое правило), в детали правила — заполнен.
 */
public record GrammarView(Grammar grammar, MasteryStatus status, List<WordRef> illustratedBy) {
}
