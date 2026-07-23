package ru.eunoia.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.Avatar;
import ru.eunoia.persistence.entity.AvatarEntity;
import ru.eunoia.persistence.repository.AvatarJpaRepository;

/** Адаптер хранилища аватаров: доменный Avatar ↔ строка avatars (bytea) поверх замоканного репозитория. */
@ExtendWith(MockitoExtension.class)
class AvatarStorageAdapterTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AvatarJpaRepository jpaRepository;

    private AvatarStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AvatarStorageAdapter(jpaRepository);
    }

    @Test
    void store_persistsEntityWithUserIdContentTypeAndTimestamp() {
        byte[] content = {1, 2, 3};
        Avatar avatar = new Avatar(content, "image/png");

        adapter.store(USER_ID, avatar);

        ArgumentCaptor<AvatarEntity> captor = ArgumentCaptor.forClass(AvatarEntity.class);
        verify(jpaRepository).save(captor.capture());
        AvatarEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getContentType()).isEqualTo("image/png");
        // время загрузки проставляется адаптером (now), а не приходит из домена
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void load_found_mapsEntityToAvatar() {
        byte[] content = {4, 5, 6};
        AvatarEntity entity = new AvatarEntity();
        entity.setUserId(USER_ID);
        entity.setContent(content);
        entity.setContentType("image/webp");
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.of(entity));

        Optional<Avatar> result = adapter.load(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo(content);
        assertThat(result.get().contentType()).isEqualTo("image/webp");
    }

    @Test
    void load_missing_returnsEmpty() {
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.load(USER_ID)).isEmpty();
    }

    @Test
    void delete_delegatesToDeleteById() {
        adapter.delete(USER_ID);

        verify(jpaRepository).deleteById(USER_ID);
    }
}
