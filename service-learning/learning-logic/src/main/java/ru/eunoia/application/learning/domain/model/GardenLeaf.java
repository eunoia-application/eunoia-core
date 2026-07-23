package ru.eunoia.application.learning.domain.model;

import ru.eunoia.application.garden.domain.model.MasteryStatus;
import ru.eunoia.application.knowledge.domain.model.LexemeRef;

/** Слово-лист темы с моим статусом мастерства — из этого фронт красит сад. */
public record GardenLeaf(LexemeRef lexeme, MasteryStatus status) {
}
