package ru.eunoia.persistence.knowledge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

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
}
