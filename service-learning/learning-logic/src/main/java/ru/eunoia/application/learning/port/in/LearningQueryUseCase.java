package ru.eunoia.application.learning.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.learning.domain.model.Band;
import ru.eunoia.application.learning.domain.model.GrammarView;
import ru.eunoia.application.learning.domain.model.TopicView;
import ru.eunoia.application.learning.domain.model.WordCard;
import ru.eunoia.application.learning.domain.model.WordLeaf;
import ru.eunoia.application.learning.domain.model.WordPage;

/** Чтение учебного контента с наложением прогресса (garden-view). Единица — слово (лемма). */
public interface LearningQueryUseCase {

    /** Карточка слова (лемма + части речи + связи) + мой статус. Пусто, если слова нет. */
    Optional<WordCard> wordCard(UUID userId, String lemmaKey);

    /** Поиск слов по префиксу леммы. */
    List<WordRef> searchWords(String query, int limit);

    List<Topic> topicRoots();

    /** Тема + её слова (леммы) с раскраской по мастерству. Пусто, если темы нет. */
    Optional<TopicView> topicView(UUID userId, String topicId);

    /** Блоки топ-слов (уровни, эксклюзивные диапазоны рангов) с моим прогрессом. */
    List<Band> bands(UUID userId);

    /** Слова блока {@code band} (или весь список по частоте, если band пуст), окно offset/limit. */
    WordPage listWords(UUID userId, String band, int offset, int limit);

    /** Мой список на изучение — слова, отмеченные «Учить» (LEARNING). */
    List<WordLeaf> study(UUID userId);

    /** Правило + слова-примеры. Пусто, если правила нет. */
    Optional<GrammarView> grammar(String grammarId);

    /** Весь ствол грамматики: правила по возрастанию CEFR. */
    List<GrammarView> grammarTrunk();
}
