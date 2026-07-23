package ru.eunoia.persistence.garden.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Составной ключ mastery: (userId, lexemeId). Имена полей совпадают с @Id в MasteryEntity. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MasteryId implements Serializable {

    private UUID userId;
    private String lexemeId;
}
