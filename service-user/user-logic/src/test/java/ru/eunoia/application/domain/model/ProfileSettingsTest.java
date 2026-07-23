package ru.eunoia.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Настройки профиля: значения по умолчанию, аксессоры и enum'ы. */
class ProfileSettingsTest {

    @Test
    void defaults_areAutoThemePrivateWithNotificationsAndAi() {
        ProfileSettings s = ProfileSettings.defaults();

        assertThat(s.theme()).isEqualTo(ProfileSettings.Theme.AUTO);
        assertThat(s.interfaceLanguage()).isNull();
        assertThat(s.profileVisibility()).isEqualTo(ProfileSettings.Visibility.PRIVATE);
        assertThat(s.emailNotifications()).isTrue();
        assertThat(s.aiSuggestionsEnabled()).isTrue();
    }

    @Test
    void accessors_returnConstructorValues() {
        ProfileSettings s = new ProfileSettings(
                ProfileSettings.Theme.DARK, "ru", ProfileSettings.Visibility.PUBLIC, false, false);

        assertThat(s.theme()).isEqualTo(ProfileSettings.Theme.DARK);
        assertThat(s.interfaceLanguage()).isEqualTo("ru");
        assertThat(s.profileVisibility()).isEqualTo(ProfileSettings.Visibility.PUBLIC);
        assertThat(s.emailNotifications()).isFalse();
        assertThat(s.aiSuggestionsEnabled()).isFalse();
    }

    @Test
    void themeEnum_hasAllValues() {
        assertThat(ProfileSettings.Theme.values())
                .containsExactly(ProfileSettings.Theme.LIGHT,
                        ProfileSettings.Theme.DARK, ProfileSettings.Theme.AUTO);
        assertThat(ProfileSettings.Theme.valueOf("DARK")).isEqualTo(ProfileSettings.Theme.DARK);
    }

    @Test
    void visibilityEnum_hasAllValues() {
        assertThat(ProfileSettings.Visibility.values())
                .containsExactly(ProfileSettings.Visibility.PUBLIC,
                        ProfileSettings.Visibility.PRIVATE);
        assertThat(ProfileSettings.Visibility.valueOf("PUBLIC"))
                .isEqualTo(ProfileSettings.Visibility.PUBLIC);
    }
}
