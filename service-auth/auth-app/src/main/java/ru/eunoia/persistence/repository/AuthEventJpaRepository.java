package ru.eunoia.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.eunoia.persistence.entity.AuthEventEntity;

@Repository
public interface AuthEventJpaRepository extends JpaRepository<AuthEventEntity, UUID> {
}
