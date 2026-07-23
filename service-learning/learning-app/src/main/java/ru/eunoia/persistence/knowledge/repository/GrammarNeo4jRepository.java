package ru.eunoia.persistence.knowledge.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import ru.eunoia.persistence.knowledge.entity.GrammarNode;

/** Репозиторий грамматических правил — пока только унаследованный findById. */
public interface GrammarNeo4jRepository extends Neo4jRepository<GrammarNode, String> {
}
