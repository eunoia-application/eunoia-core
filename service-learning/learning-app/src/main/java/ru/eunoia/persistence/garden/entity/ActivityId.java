package ru.eunoia.persistence.garden.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Составной ключ журнала занятий: (userId, day). Имена полей совпадают с @Id в ActivityEntity. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ActivityId implements Serializable {

    private UUID userId;
    private LocalDate day;
}
