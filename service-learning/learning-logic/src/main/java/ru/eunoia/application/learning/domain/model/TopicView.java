package ru.eunoia.application.learning.domain.model;

import java.util.List;
import ru.eunoia.application.knowledge.domain.model.Topic;

/** Ветка сада: тема + её слова (леммы) с раскраской по мастерству (структура × статус). */
public record TopicView(Topic topic, List<WordLeaf> words) {
}
