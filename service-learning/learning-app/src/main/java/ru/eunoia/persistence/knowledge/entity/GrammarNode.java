package ru.eunoia.persistence.knowledge.entity;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/** Узел грамматического правила. id — бизнес-id из импорта (присвоенный). cefr строкой — enum в адаптере. */
@Node("Grammar")
@Getter
@Setter
@NoArgsConstructor
public class GrammarNode {

    @Id
    private String id;

    private String name;
    private String cefr;

    /** Правила-предшественники (входящие рёбра PREREQUISITE): их желательно знать раньше. */
    @Relationship(type = "PREREQUISITE", direction = Relationship.Direction.INCOMING)
    private List<GrammarNode> prerequisites;
}
