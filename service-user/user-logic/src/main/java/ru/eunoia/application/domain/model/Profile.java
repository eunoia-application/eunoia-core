package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Профиль пользователя (Design B: identity — в auth, профиль — здесь).
 * userId/email/username приходят из auth событием UserRegistered; остальное правит сам пользователь.
 */
public record Profile(
        UUID userId,
        String email,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        boolean emailVerified,
        ProfileSettings settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** Новый профиль при регистрации: базовые поля из события, остальное — по умолчанию. */
    public static Profile createFor(UUID userId, String email, String username) {
        LocalDateTime now = LocalDateTime.now();
        return new Profile(userId, email, username, null, null, null, null,
                false, ProfileSettings.defaults(), now, now);
    }

    /** Обновление отображаемых данных (имя, био, аватар-URL). */
    public Profile withProfile(String firstName, String lastName, String bio, String avatarUrl) {
        return new Profile(userId, email, username, firstName, lastName, avatarUrl, bio,
                emailVerified, settings, createdAt, LocalDateTime.now());
    }

    /** Только аватар (загрузка/удаление — url=null). */
    public Profile withAvatar(String url) {
        return new Profile(userId, email, username, firstName, lastName, url, bio,
                emailVerified, settings, createdAt, LocalDateTime.now());
    }

    public Profile withSettings(ProfileSettings newSettings) {
        return new Profile(userId, email, username, firstName, lastName, avatarUrl, bio,
                emailVerified, newSettings, createdAt, LocalDateTime.now());
    }

    /** Виден ли профиль другим (для GET /users/{id}). */
    public boolean isPublic() {
        return settings != null && settings.profileVisibility() == ProfileSettings.Visibility.PUBLIC;
    }
}
