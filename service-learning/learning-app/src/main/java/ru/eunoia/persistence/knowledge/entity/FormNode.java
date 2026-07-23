package ru.eunoia.persistence.knowledge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Словоформа — «лист» слова, своего бизнес-id не имеет, поэтому id генерит база
 * (@GeneratedValue → внутренний elementId Neo4j 5).
 */
@Node("Form")
@Getter
@Setter
@NoArgsConstructor
public class FormNode {

    @Id
    @GeneratedValue
    private String id;

    private String text;
    private String feature;
}
