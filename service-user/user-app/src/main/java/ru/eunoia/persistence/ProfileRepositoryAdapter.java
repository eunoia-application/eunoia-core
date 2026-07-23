package ru.eunoia.persistence;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.out.ProfileRepositoryPort;
import ru.eunoia.persistence.entity.ProfileEntity;
import ru.eunoia.persistence.repository.ProfileJpaRepository;

/** JPA-адаптер профиля. userId присвоенный (из auth), не генерим. Настройки ↔ колонки вручную. */
@Component
@RequiredArgsConstructor
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private final ProfileJpaRepository jpaRepository;

    @Override
    public Profile save(Profile profile) {
        return toDomain(jpaRepository.save(toEntity(profile)));
    }

    @Override
    public Optional<Profile> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsById(userId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteById(userId);
    }

    private ProfileEntity toEntity(Profile p) {
        ProfileEntity e = new ProfileEntity();
        e.setUserId(p.userId());
        e.setEmail(p.email());
        e.setUsername(p.username());
        e.setFirstName(p.firstName());
        e.setLastName(p.lastName());
        e.setAvatarUrl(p.avatarUrl());
        e.setBio(p.bio());
        e.setEmailVerified(p.emailVerified());
        ProfileSettings s = p.settings();
        e.setTheme(s.theme().name());
        e.setInterfaceLanguage(s.interfaceLanguage());
        e.setProfileVisibility(s.profileVisibility().name());
        e.setEmailNotifications(s.emailNotifications());
        e.setAiSuggestionsEnabled(s.aiSuggestionsEnabled());
        e.setCreatedAt(p.createdAt());
        e.setUpdatedAt(p.updatedAt());
        return e;
    }

    private Profile toDomain(ProfileEntity e) {
        ProfileSettings settings = new ProfileSettings(
                ProfileSettings.Theme.valueOf(e.getTheme()),
                e.getInterfaceLanguage(),
                ProfileSettings.Visibility.valueOf(e.getProfileVisibility()),
                e.isEmailNotifications(),
                e.isAiSuggestionsEnabled());
        return new Profile(e.getUserId(), e.getEmail(), e.getUsername(),
                e.getFirstName(), e.getLastName(), e.getAvatarUrl(), e.getBio(),
                e.isEmailVerified(), settings, e.getCreatedAt(), e.getUpdatedAt());
    }
}
