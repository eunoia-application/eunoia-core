package ru.eunoia.persistence.garden.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Строка журнала занятий: (userId, day) → сколько действий было в этот день. Нужна дереву для
 * стриков/погоды/сезонов. Пишется на каждое действие мастерства (см. MasteryUseCaseImpl).
 */
@Entity
@Table(name = "activity_log")
@IdClass(ActivityId.class)
@Getter
@Setter
public class ActivityEntity {

    @Id
    private UUID userId;
    @Id
    private LocalDate day;

    @Column(nullable = false)
    private int actions;
}
