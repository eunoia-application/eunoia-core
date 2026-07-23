package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Доменная модель профиля: фабрика createFor, with-методы и признак публичности. */
class ProfileTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED = LocalDateTime.now().minusDays(2);
    private static final LocalDateTime UPDATED = LocalDateTime.now().minusDays(1);

    /** Профиль с известными значениями и заданными настройками. */
    private static Profile profileWith(ProfileSettings settings) {
        return new Profile(USER_ID, "u@eunoia.ru", "user", "First", "Last",
                "http://ava", "bio", true, settings, CREATED, UPDATED);
    }

    @Test
    void createFor_setsBaseFieldsAndDefaults() {
        LocalDateTime before = LocalDateTime.now();
        Profile p = Profile.createFor(USER_ID, "u@eunoia.ru", "user");
        LocalDateTime after = LocalDateTime.now();

        assertThat(p.userId()).isEqualTo(USER_ID);
        assertThat(p.email()).isEqualTo("u@eunoia.ru");
        assertThat(p.username()).isEqualTo("user");
        assertThat(p.firstName()).isNull();
        assertThat(p.lastName()).isNull();
        assertThat(p.avatarUrl()).isNull();
        assertThat(p.bio()).isNull();
        assertThat(p.emailVerified()).isFalse();
        assertThat(p.settings()).isEqualTo(ProfileSettings.defaults());
        assertThat(p.createdAt()).isBetween(before, after);
        assertThat(p.updatedAt()).isBetween(before, after);
    }

    @Test
    void withProfile_replacesDisplayFieldsAndStampsUpdatedAt() {
        Profile base = profileWith(ProfileSettings.defaults());

        LocalDateTime before = LocalDateTime.now();
        Profile r = base.withProfile("Neo", "Anderson", "hacker", "http://new");
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(base);
        assertThat(r.firstName()).isEqualTo("Neo");
        assertThat(r.lastName()).isEqualTo("Anderson");
        assertThat(r.bio()).isEqualTo("hacker");
        assertThat(r.avatarUrl()).isEqualTo("http://new");
        assertThat(r.userId()).isEqualTo(USER_ID);
        assertThat(r.createdAt()).isEqualTo(CREATED);
        assertThat(r.updatedAt()).isBetween(before, after);
    }

    @Test
    void withAvatar_setsUrlAndStampsUpdatedAt() {
        Profile base = profileWith(ProfileSettings.defaults());

        LocalDateTime before = LocalDateTime.now();
        Profile r = base.withAvatar("http://avatar2");
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(base);
        assertThat(r.avatarUrl()).isEqualTo("http://avatar2");
        assertThat(r.createdAt()).isEqualTo(CREATED);
        assertThat(r.updatedAt()).isBetween(before, after);
    }

    @Test
    void withAvatar_null_clearsAvatar() {
        Profile base = profileWith(ProfileSettings.defaults());

        Profile r = base.withAvatar(null);

        assertThat(r.avatarUrl()).isNull();
    }

    @Test
    void withSettings_replacesSettingsAndStampsUpdatedAt() {
        Profile base = profileWith(ProfileSettings.defaults());
        ProfileSettings newSettings = new ProfileSettings(
                ProfileSettings.Theme.DARK, "ru", ProfileSettings.Visibility.PUBLIC, false, false);

        LocalDateTime before = LocalDateTime.now();
        Profile r = base.withSettings(newSettings);
        LocalDateTime after = LocalDateTime.now();

        assertThat(r).isNotSameAs(base);
        assertThat(r.settings()).isSameAs(newSettings);
        assertThat(r.createdAt()).isEqualTo(CREATED);
        assertThat(r.updatedAt()).isBetween(before, after);
    }

    @Test
    void isPublic_falseWhenSettingsNull() {
        Profile p = new Profile(USER_ID, "u@eunoia.ru", "user", null, null, null, null,
                false, null, CREATED, UPDATED);

        assertThat(p.isPublic()).isFalse();
    }

    @Test
    void isPublic_falseWhenVisibilityPrivate() {
        assertThat(profileWith(ProfileSettings.defaults()).isPublic()).isFalse();
    }

    @Test
    void isPublic_trueWhenVisibilityPublic() {
        Profile p = profileWith(new ProfileSettings(
                ProfileSettings.Theme.AUTO, null, ProfileSettings.Visibility.PUBLIC, true, true));

        assertThat(p.isPublic()).isTrue();
    }
}
