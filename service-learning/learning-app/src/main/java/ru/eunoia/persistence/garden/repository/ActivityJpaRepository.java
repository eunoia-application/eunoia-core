package ru.eunoia.persistence.garden.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.eunoia.persistence.garden.entity.ActivityEntity;
import ru.eunoia.persistence.garden.entity.ActivityId;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, ActivityId> {

    /** Upsert дня активности: вставить со счётчиком 1 либо +1 к существующему (атомарно). */
    @Modifying
    @Query(value = "INSERT INTO activity_log (user_id, day, actions) VALUES (:userId, :day, 1) "
            + "ON CONFLICT (user_id, day) DO UPDATE SET actions = activity_log.actions + 1",
            nativeQuery = true)
    void touch(@Param("userId") UUID userId, @Param("day") LocalDate day);

    /** Активные дни пользователя от даты (включительно). */
    @Query("SELECT a.day FROM ActivityEntity a WHERE a.userId = :userId AND a.day >= :since")
    List<LocalDate> activeDaysSince(@Param("userId") UUID userId, @Param("since") LocalDate since);
}
