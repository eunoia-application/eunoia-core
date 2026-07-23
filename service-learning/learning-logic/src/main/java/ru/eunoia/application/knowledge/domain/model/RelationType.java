package ru.eunoia.application.knowledge.domain.model;

/** Тип связи между словами. Имена совпадают с типами рёбер в Neo4j. */
public enum RelationType {
    SYNONYM,
    ANTONYM,
    HYPERNYM
}
