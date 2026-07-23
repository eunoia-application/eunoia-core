package ru.eunoia.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.eunoia.persistence.entity.ProfileEntity;

@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {
}
