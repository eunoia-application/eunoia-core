package ru.eunoia.persistence.knowledge.entity;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Узел слова в графе. id — бизнес-id из импорта (присвоенный, не генерим). pos/cefr лежат
 * строками — enum'ы домена собираем в адаптере. Формы и переводы — «листья» слова
 * (исходящие рёбра), их SDN грузит вместе с узлом. Связи между словами (синонимы и т.п.)
 * узлом не тянем — они отдаются отдельными запросами, чтобы не грузить весь граф.
 */
@Node("Lexeme")
@Getter
@Setter
@NoArgsConstructor
public class LexemeNode {

    @Id
    private String id;

    private String lemma;
    private String pos;
    private String lang;
    private String cefr;
    private Integer freqRank;

    @Relationship(type = "HAS_FORM", direction = Relationship.Direction.OUTGOING)
    private List<FormNode> forms;

    @Relationship(type = "TRANSLATION", direction = Relationship.Direction.OUTGOING)
    private List<TranslationNode> translations;
}
