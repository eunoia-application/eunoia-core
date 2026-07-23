package ru.eunoia.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.eunoia.persistence.entity.AvatarEntity;

public interface AvatarJpaRepository extends JpaRepository<AvatarEntity, UUID> {
}
