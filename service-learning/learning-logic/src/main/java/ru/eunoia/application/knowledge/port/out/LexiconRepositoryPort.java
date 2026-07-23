package ru.eunoia.application.knowledge.port.out;

import java.util.List;
import java.util.Optional;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Lexeme;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;
import ru.eunoia.application.knowledge.domain.model.RelationType;
import ru.eunoia.application.knowledge.domain.model.Topic;

/**
 * Чтение канонического графа знаний. Реализуется Neo4j-адаптером в learning-app.
 * Полное слово — только {@link #findById}; связи/поиск/слова темы отдаём лёгкими {@link LexemeRef}.
 */
public interface LexiconRepositoryPort {

    /** Слово с формами и переводами (карточка). */
    Optional<Lexeme> findById(String id);

    /** Поиск слов по префиксу леммы (для строки поиска). */
    List<LexemeRef> search(String query, int limit);

    /** Связанные слова заданного типа (синонимы/антонимы/гиперонимы). */
    List<LexemeRef> related(String lexemeId, RelationType type);

    /** Корневые темы (верх дерева тем). */
    List<Topic> topicRoots();

    Optional<Topic> findTopic(String id);

    /** Слова, привязанные к теме. */
    List<LexemeRef> lexemesInTopic(String topicId);

    Optional<Grammar> findGrammar(String id);
}
