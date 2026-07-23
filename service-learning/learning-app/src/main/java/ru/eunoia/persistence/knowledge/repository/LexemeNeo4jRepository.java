package ru.eunoia.persistence.knowledge.repository;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import ru.eunoia.persistence.knowledge.entity.LexemeNode;

/**
 * Репозиторий слов. Унаследованный {@code findById} SDN достраивает формами и переводами
 * (карточка слова). Остальные запросы возвращают «голые» узлы Lexeme — адаптер отдаст их
 * лёгкими ссылками, без форм/переводов. {@code @Param} — чтобы биндинг не зависел от -parameters.
 */
public interface LexemeNeo4jRepository extends Neo4jRepository<LexemeNode, String> {

    /** Поиск по префиксу леммы, самые частотные — выше. */
    @Query("MATCH (l:Lexeme) WHERE l.lemma STARTS WITH $query RETURN l ORDER BY l.freqRank ASC LIMIT $limit")
    List<LexemeNode> searchByLemmaPrefix(@Param("query") String query, @Param("limit") int limit);

    /** Связанные слова заданного типа ребра (SYNONYM/ANTONYM/HYPERNYM). Матч ненаправленный — ok для M4.2. */
    @Query("MATCH (l:Lexeme {id: $id})-[r]-(o:Lexeme) WHERE type(r) = $type RETURN o ORDER BY o.freqRank ASC")
    List<LexemeNode> relatedByType(@Param("id") String id, @Param("type") String type);

    /** Слова, привязанные к теме ребром IN_TOPIC. */
    @Query("MATCH (:Topic {id: $topicId})<-[:IN_TOPIC]-(l:Lexeme) RETURN l ORDER BY l.freqRank ASC")
    List<LexemeNode> inTopic(@Param("topicId") String topicId);
}
