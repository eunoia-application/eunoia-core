package ru.eunoia.application.learning.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.learning.domain.model.LexemeCard;
import ru.eunoia.application.learning.domain.model.TopicView;

/** Чтение учебного контента с наложением персонального прогресса (garden-view). */
public interface LearningQueryUseCase {

    /** Карточка слова + связи + мой статус. Пусто, если слова нет. */
    Optional<LexemeCard> lexemeCard(UUID userId, String lexemeId);

    List<LexemeRef> search(String query, int limit);

    List<Topic> topicRoots();

    /** Тема + её слова с раскраской по мастерству. Пусто, если темы нет. */
    Optional<TopicView> topicView(UUID userId, String topicId);

    Optional<Grammar> grammar(String grammarId);
}
