package ru.eunoia.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.eunoia.persistence.entity.OneTimeTokenEntity;

public interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeTokenEntity, UUID> {

    Optional<OneTimeTokenEntity> findByTokenHash(String tokenHash);
}
