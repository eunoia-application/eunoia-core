package ru.eunoia.persistence.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.eunoia.persistence.entity.RefreshTokenEntity;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revoked = true, r.revokedAt = :now "
            + "where r.userId = :userId and r.revoked = false")
    void revokeActiveByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
