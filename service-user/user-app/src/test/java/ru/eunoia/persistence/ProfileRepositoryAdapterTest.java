package ru.eunoia.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.persistence.entity.ProfileEntity;
import ru.eunoia.persistence.repository.ProfileJpaRepository;

/** Адаптер вручную раскладывает настройки в строковые колонки и собирает их обратно — проверяем маппинг в обе стороны. */
@ExtendWith(MockitoExtension.class)
class ProfileRepositoryAdapterTest {

    @Mock
    private ProfileJpaRepository jpaRepository;

    private ProfileRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProfileRepositoryAdapter(jpaRepository);
    }

    /** Полностью заполненный домен-профиль с заданными настройками. */
    private static Profile profileWith(ProfileSettings settings) {
        return new Profile(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "user@eunoia.ru", "user", "Иван", "Петров",
                "https://cdn.eunoia.ru/a.png", "коротко о себе", true,
                settings,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 2, 2, 12, 30));
    }

    /** Строка profiles с заданными темой/приватностью (остальное — фикс). */
    private static ProfileEntity entityWith(UUID userId, String theme, String visibility) {
        ProfileEntity e = new ProfileEntity();
        e.setUserId(userId);
        e.setEmail("found@eunoia.ru");
        e.setUsername("found");
        e.setFirstName("Анна");
        e.setLastName("Смирнова");
        e.setAvatarUrl("https://cdn.eunoia.ru/f.png");
        e.setBio("bio");
        e.setEmailVerified(true);
        e.setTheme(theme);
        e.setInterfaceLanguage("en");
        e.setProfileVisibility(visibility);
        e.setEmailNotifications(false);
        e.setAiSuggestionsEnabled(true);
        e.setCreatedAt(LocalDateTime.of(2026, 3, 3, 9, 15));
        e.setUpdatedAt(LocalDateTime.of(2026, 4, 4, 18, 45));
        return e;
    }

    @Test
    void save_mapsSettingsToStringColumnsAndReturnsDomain() {
        Profile profile = profileWith(new ProfileSettings(
                ProfileSettings.Theme.DARK, "ru", ProfileSettings.Visibility.PUBLIC, false, true));
        when(jpaRepository.save(any(ProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = adapter.save(profile);

        ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(jpaRepository).save(captor.capture());
        ProfileEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(profile.userId());
        assertThat(saved.getEmail()).isEqualTo("user@eunoia.ru");
        assertThat(saved.getUsername()).isEqualTo("user");
        assertThat(saved.getFirstName()).isEqualTo("Иван");
        assertThat(saved.getLastName()).isEqualTo("Петров");
        assertThat(saved.getAvatarUrl()).isEqualTo("https://cdn.eunoia.ru/a.png");
        assertThat(saved.getBio()).isEqualTo("коротко о себе");
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getTheme()).isEqualTo("DARK");
        assertThat(saved.getInterfaceLanguage()).isEqualTo("ru");
        assertThat(saved.getProfileVisibility()).isEqualTo("PUBLIC");
        assertThat(saved.isEmailNotifications()).isFalse();
        assertThat(saved.isAiSuggestionsEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isEqualTo(profile.createdAt());
        assertThat(saved.getUpdatedAt()).isEqualTo(profile.updatedAt());

        // обратный маппing сохранённой строки полностью восстанавливает домен
        assertThat(result).isEqualTo(profile);
    }

    @Test
    void save_roundTripsDefaultSettings() {
        Profile profile = profileWith(ProfileSettings.defaults());
        when(jpaRepository.save(any(ProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = adapter.save(profile);

        ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(jpaRepository).save(captor.capture());
        ProfileEntity saved = captor.getValue();
        assertThat(saved.getTheme()).isEqualTo("AUTO");
        assertThat(saved.getProfileVisibility()).isEqualTo("PRIVATE");
        assertThat(saved.getInterfaceLanguage()).isNull();
        assertThat(saved.isEmailNotifications()).isTrue();
        assertThat(saved.isAiSuggestionsEnabled()).isTrue();

        assertThat(result).isEqualTo(profile);
    }

    @Test
    void findByUserId_found_mapsEntityToDomain() {
        UUID userId = UUID.randomUUID();
        ProfileEntity entity = entityWith(userId, "LIGHT", "PRIVATE");
        when(jpaRepository.findById(userId)).thenReturn(Optional.of(entity));

        Optional<Profile> result = adapter.findByUserId(userId);

        assertThat(result).isPresent();
        Profile profile = result.orElseThrow();
        assertThat(profile.userId()).isEqualTo(userId);
        assertThat(profile.email()).isEqualTo("found@eunoia.ru");
        assertThat(profile.username()).isEqualTo("found");
        assertThat(profile.firstName()).isEqualTo("Анна");
        assertThat(profile.lastName()).isEqualTo("Смирнова");
        assertThat(profile.avatarUrl()).isEqualTo("https://cdn.eunoia.ru/f.png");
        assertThat(profile.bio()).isEqualTo("bio");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(profile.settings().theme()).isEqualTo(ProfileSettings.Theme.LIGHT);
        assertThat(profile.settings().interfaceLanguage()).isEqualTo("en");
        assertThat(profile.settings().profileVisibility()).isEqualTo(ProfileSettings.Visibility.PRIVATE);
        assertThat(profile.settings().emailNotifications()).isFalse();
        assertThat(profile.settings().aiSuggestionsEnabled()).isTrue();
        assertThat(profile.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(profile.updatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    void findByUserId_notFound_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(adapter.findByUserId(userId)).isEmpty();
    }

    @Test
    void existsByUserId_trueWhenPresent() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.existsById(userId)).thenReturn(true);

        assertThat(adapter.existsByUserId(userId)).isTrue();
        verify(jpaRepository).existsById(userId);
    }

    @Test
    void existsByUserId_falseWhenAbsent() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.existsById(userId)).thenReturn(false);

        assertThat(adapter.existsByUserId(userId)).isFalse();
    }

    @Test
    void deleteByUserId_delegatesToJpaRepository() {
        UUID userId = UUID.randomUUID();

        adapter.deleteByUserId(userId);

        verify(jpaRepository).deleteById(userId);
    }
}
