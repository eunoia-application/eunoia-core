package ru.eunoia.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.Avatar;
import ru.eunoia.application.port.out.AvatarStoragePort;
import ru.eunoia.persistence.entity.AvatarEntity;
import ru.eunoia.persistence.repository.AvatarJpaRepository;

/** JPA-адаптер хранилища аватаров (Postgres bytea). userId присвоенный, не генерим. */
@Component
@RequiredArgsConstructor
public class AvatarStorageAdapter implements AvatarStoragePort {

    private final AvatarJpaRepository jpaRepository;

    @Override
    public void store(UUID userId, Avatar avatar) {
        AvatarEntity entity = new AvatarEntity();
        entity.setUserId(userId);
        entity.setContent(avatar.content());
        entity.setContentType(avatar.contentType());
        entity.setUpdatedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Avatar> load(UUID userId) {
        return jpaRepository.findById(userId)
                .map(e -> new Avatar(e.getContent(), e.getContentType()));
    }

    @Override
    public void delete(UUID userId) {
        jpaRepository.deleteById(userId);
    }
}
