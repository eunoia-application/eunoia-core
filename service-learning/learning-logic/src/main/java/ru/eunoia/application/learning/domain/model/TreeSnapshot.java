package ru.eunoia.application.learning.domain.model;

import java.util.List;
import ru.eunoia.application.garden.domain.model.ActivityStats;

/**
 * Снапшот сада для рендера дерева знаний одним махом: словарь (листья), ветки-темы (рост ветвей),
 * активность (погода/сезоны/стрик), грамматика (высота ствола). Джойн канона (Neo4j) × оверлея
 * (Postgres) в фасаде. Числа непрерывные — стадии/высоту/цвет считает фронт.
 */
public record TreeSnapshot(TreeVocabulary vocabulary, List<TreeTopic> topics, ActivityStats activity,
                           TreeGrammar grammar) {
}
