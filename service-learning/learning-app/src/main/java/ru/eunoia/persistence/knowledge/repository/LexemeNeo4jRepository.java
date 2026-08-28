package ru.eunoia.persistence.knowledge.repository;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import ru.eunoia.persistence.knowledge.entity.LexemeNode;

/**
 * Репозиторий слов-узлов. {@code findByIdStartingWith} — все части речи одной леммы (SDN
 * достраивает формами/переводами): по нему адаптер собирает карточку-слово. Списки по темам/всем
 * словам — агрегатные (distinct-лемма), их адаптер делает через Neo4jClient, не тут.
 * {@code @Param} — чтобы биндинг не зависел от -parameters.
 */
public interface LexemeNeo4jRepository extends Neo4jRepository<LexemeNode, String> {

    /** Все части речи одной леммы: id вида "en:go:VERB" по префиксу "en:go:" (формы/переводы гидрируются). */
    List<LexemeNode> findByIdStartingWith(String idPrefix);

    /**
     * Связанные слова заданного типа ребра (SYNONYM/ANTONYM/HYPERNYM). Матч ненаправленный, но
     * DISTINCT — иначе взаимное ребро (a→b и b→a) вернуло бы одно и то же слово дважды.
     */
    @Query("MATCH (l:Lexeme {id: $id})-[r]-(o:Lexeme) WHERE type(r) = $type "
            + "RETURN DISTINCT o ORDER BY o.freqRank ASC")
    List<LexemeNode> relatedByType(@Param("id") String id, @Param("type") String type);

    /** Слова, иллюстрирующие грамматическое правило (ребро ILLUSTRATES). */
    @Query("MATCH (l:Lexeme)-[:ILLUSTRATES]->(:Grammar {id: $id}) RETURN l ORDER BY l.freqRank ASC")
    List<LexemeNode> illustrating(@Param("id") String grammarId);
}
