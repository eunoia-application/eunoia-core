package ru.eunoia.persistence.garden.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Составной ключ grammar_mastery: (userId, grammarId). Имена полей совпадают с @Id в сущности. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GrammarMasteryId implements Serializable {

    private UUID userId;
    private String grammarId;
}
