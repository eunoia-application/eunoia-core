package ru.eunoia.persistence.knowledge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Узел темы. id — бизнес-id из импорта (присвоенный). Ветвление тем (SUBTOPIC) — в запросах. */
@Node("Topic")
@Getter
@Setter
@NoArgsConstructor
public class TopicNode {

    @Id
    private String id;

    private String name;
    private String slug;
}
