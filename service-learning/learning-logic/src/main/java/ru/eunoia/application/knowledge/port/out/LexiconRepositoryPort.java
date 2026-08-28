package ru.eunoia.application.knowledge.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.eunoia.application.knowledge.domain.model.Grammar;
import ru.eunoia.application.knowledge.domain.model.Topic;
import ru.eunoia.application.knowledge.domain.model.Word;
import ru.eunoia.application.knowledge.domain.model.WordRef;
import ru.eunoia.application.knowledge.domain.model.WordSummary;

/**
 * Чтение канонического графа знаний. Единица — СЛОВО (лемма): {@link #findWord} склеивает части
 * речи в один {@link Word} с variants; списки (поиск/тема/все) отдают лёгкие сводки/ссылки.
 * Реализуется Neo4j-адаптером в learning-app.
 */
public interface LexiconRepositoryPort {

    /** Слово (лемма) целиком: части речи + формы/переводы/связи. lemmaKey вида "en:go". */
    Optional<Word> findWord(String lemmaKey);

    /** Поиск слов по префиксу леммы (лёгкие ссылки, без части речи). */
    List<WordRef> searchWords(String query, int limit);

    /** Корневые темы (верх дерева тем). */
    List<Topic> topicRoots();

    /** Число слов (лемм) в каждой ветке-теме: topicId → count — для прогресса веток дерева. */
    Map<String, Long> topicWordCounts();

    Optional<Topic> findTopic(String id);

    /** Слова темы (леммы) — лёгкие сводки для листьев сада. */
    List<WordSummary> wordsInTopic(String topicId);

    /** Все слова по возрастанию частоты, окно offset/limit — для «Все слова». */
    List<WordSummary> allWords(int offset, int limit);

    /** Сколько всего уникальных слов (лемм) — total для страницы. */
    long countWords();

    /** Слова в диапазоне рангов [from, to] (окно offset/limit), по частоте — для блока. */
    List<WordSummary> wordsInRank(int fromRank, int toRank, int offset, int limit);

    /** Сколько уникальных слов (лемм) в диапазоне рангов [from, to] — total блока. */
    long countWordsInRank(int fromRank, int toRank);

    /** Ранг (минимум по частям речи) для набора лемм по их ключам — для прогресса блоков. */
    Map<String, Integer> ranksOf(Collection<String> lemmaKeys);

    /** Сводки слов по набору ключей лемм (для списка «Учить»). */
    List<WordSummary> wordsByKeys(Collection<String> lemmaKeys);

    /** Правило по id (с порядком PREREQUISITE). */
    Optional<Grammar> findGrammar(String id);

    /** Весь ствол грамматики: правила по возрастанию CEFR (с их предшественниками). */
    List<Grammar> grammarTrunk();

    /** Сколько всего правил в стволе — total для «высоты» дерева. */
    long countGrammar();

    /** Слова-примеры, иллюстрирующие правило (ILLUSTRATES). */
    List<WordRef> grammarIllustratedBy(String grammarId);
}
