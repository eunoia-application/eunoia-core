package ru.eunoia.persistence.knowledge.repository;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import ru.eunoia.persistence.knowledge.entity.TopicNode;

/** Репозиторий тем. */
public interface TopicNeo4jRepository extends Neo4jRepository<TopicNode, String> {

    /** Корни дерева тем — те, у кого нет родителя (входящего ребра SUBTOPIC). */
    @Query("MATCH (t:Topic) WHERE NOT (t)<-[:SUBTOPIC]-(:Topic) RETURN t ORDER BY t.name")
    List<TopicNode> findRoots();
}
