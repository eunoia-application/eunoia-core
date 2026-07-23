package ru.eunoia.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunoia.application.user.model.UserDataExport;
import com.eunoia.application.user.model.UserProfile;
import com.eunoia.application.user.model.UserPublicProfile;
import com.eunoia.application.user.model.UserSettings;
import com.eunoia.application.user.model.UserUpdateRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;

/**
 * Чистое преобразование домен ↔ DTO контракта. Без моков — конструируем реальные объекты.
 * Отдельно проверяем обе ветки каждого тернарника (null-значения → дефолты) и оба
 * направления avatarUrl (String ↔ java.net.URI).
 */
class UserApiMapperTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 6, 7, 8, 9, 10);

    private final UserApiMapper mapper = new UserApiMapper();

    /** Профиль с заданным аватаром и настройками; остальные поля фиксированы. */
    private static Profile profile(String avatarUrl, ProfileSettings settings) {
        return new Profile(USER_ID, "user@example.com", "eunoia_user", "Alex", "Johnson",
                avatarUrl, "Just a bio", true, settings, CREATED, UPDATED);
    }

    @Test
    void toProfile_mapsEveryField_andConvertsAvatarUrlToUri() {
        ProfileSettings settings = new ProfileSettings(ProfileSettings.Theme.LIGHT, "en",
                ProfileSettings.Visibility.PUBLIC, true, false);

        UserProfile dto = mapper.toProfile(profile("https://cdn.example.com/a.png", settings));

        assertThat(dto.getId()).isEqualTo(USER_ID);
        assertThat(dto.getEmail()).isEqualTo("user@example.com");
        assertThat(dto.getUsername()).isEqualTo("eunoia_user");
        assertThat(dto.getFirstName()).isEqualTo("Alex");
        assertThat(dto.getLastName()).isEqualTo("Johnson");
        assertThat(dto.getAvatarUrl()).isEqualTo(URI.create("https://cdn.example.com/a.png"));
        assertThat(dto.getBio()).isEqualTo("Just a bio");
        assertThat(dto.getEmailVerified()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(CREATED);
        assertThat(dto.getUpdatedAt()).isEqualTo(UPDATED);
        // Настройки прогоняются через toSettingsDto — заодно ветка LIGHT/PUBLIC.
        assertThat(dto.getSettings()).isNotNull();
        assertThat(dto.getSettings().getTheme()).isEqualTo(UserSettings.ThemeEnum.LIGHT);
        assertThat(dto.getSettings().getProfileVisibility())
                .isEqualTo(UserSettings.ProfileVisibilityEnum.PUBLIC);
    }

    @Test
    void toProfile_nullAvatar_staysNull() {
        UserProfile dto = mapper.toProfile(profile(null, ProfileSettings.defaults()));

        assertThat(dto.getAvatarUrl()).isNull();
    }

    @Test
    void toPublicProfile_mapsPublicFields_andConvertsAvatarUrlToUri() {
        UserPublicProfile dto =
                mapper.toPublicProfile(profile("https://cdn.example.com/p.png", ProfileSettings.defaults()));

        assertThat(dto.getId()).isEqualTo(USER_ID);
        assertThat(dto.getUsername()).isEqualTo("eunoia_user");
        assertThat(dto.getFirstName()).isEqualTo("Alex");
        assertThat(dto.getLastName()).isEqualTo("Johnson");
        assertThat(dto.getAvatarUrl()).isEqualTo(URI.create("https://cdn.example.com/p.png"));
        assertThat(dto.getBio()).isEqualTo("Just a bio");
        assertThat(dto.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void toPublicProfile_nullAvatar_staysNull() {
        UserPublicProfile dto = mapper.toPublicProfile(profile(null, ProfileSettings.defaults()));

        assertThat(dto.getAvatarUrl()).isNull();
    }

    @Test
    void toExport_setsExportedAtNow_andEmbedsMappedProfile() {
        LocalDateTime before = LocalDateTime.now();

        UserDataExport dto =
                mapper.toExport(profile("https://cdn.example.com/a.png", ProfileSettings.defaults()));

        assertThat(dto.getExportedAt()).isNotNull();
        assertThat(dto.getExportedAt()).isAfterOrEqualTo(before);
        assertThat(dto.getProfile()).isNotNull();
        assertThat(dto.getProfile().getId()).isEqualTo(USER_ID);
        assertThat(dto.getProfile().getAvatarUrl()).isEqualTo(URI.create("https://cdn.example.com/a.png"));
    }

    @Test
    void toCommand_withAvatar_convertsUriToString() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Alex");
        req.setLastName("Johnson");
        req.setBio("bio");
        req.setAvatarUrl(URI.create("https://cdn.example.com/a.png"));

        UpdateProfileCommand cmd = mapper.toCommand(req);

        assertThat(cmd.firstName()).isEqualTo("Alex");
        assertThat(cmd.lastName()).isEqualTo("Johnson");
        assertThat(cmd.bio()).isEqualTo("bio");
        assertThat(cmd.avatarUrl()).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void toCommand_nullAvatar_staysNull() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Alex");

        UpdateProfileCommand cmd = mapper.toCommand(req);

        assertThat(cmd.avatarUrl()).isNull();
        assertThat(cmd.firstName()).isEqualTo("Alex");
        assertThat(cmd.lastName()).isNull();
        assertThat(cmd.bio()).isNull();
    }

    @Test
    void toSettings_nulls_fallBackToDefaults() {
        UserSettings dto = new UserSettings();
        dto.setTheme(null);
        dto.setInterfaceLanguage(null);
        dto.setProfileVisibility(null);
        dto.setEmailNotifications(null);
        dto.setAiSuggestionsEnabled(null);

        ProfileSettings s = mapper.toSettings(dto);

        assertThat(s.theme()).isEqualTo(ProfileSettings.Theme.AUTO);
        assertThat(s.interfaceLanguage()).isNull();
        assertThat(s.profileVisibility()).isEqualTo(ProfileSettings.Visibility.PRIVATE);
        assertThat(s.emailNotifications()).isTrue();
        assertThat(s.aiSuggestionsEnabled()).isTrue();
    }

    @Test
    void toSettings_explicitValues_true() {
        UserSettings dto = new UserSettings();
        dto.setTheme(UserSettings.ThemeEnum.DARK);
        dto.setInterfaceLanguage("ru");
        dto.setProfileVisibility(UserSettings.ProfileVisibilityEnum.PRIVATE);
        dto.setEmailNotifications(true);
        dto.setAiSuggestionsEnabled(true);

        ProfileSettings s = mapper.toSettings(dto);

        assertThat(s.theme()).isEqualTo(ProfileSettings.Theme.DARK);
        assertThat(s.interfaceLanguage()).isEqualTo("ru");
        assertThat(s.profileVisibility()).isEqualTo(ProfileSettings.Visibility.PRIVATE);
        assertThat(s.emailNotifications()).isTrue();
        assertThat(s.aiSuggestionsEnabled()).isTrue();
    }

    @Test
    void toSettings_explicitValues_false() {
        UserSettings dto = new UserSettings();
        dto.setTheme(UserSettings.ThemeEnum.LIGHT);
        dto.setInterfaceLanguage("en");
        dto.setProfileVisibility(UserSettings.ProfileVisibilityEnum.PUBLIC);
        dto.setEmailNotifications(false);
        dto.setAiSuggestionsEnabled(false);

        ProfileSettings s = mapper.toSettings(dto);

        assertThat(s.theme()).isEqualTo(ProfileSettings.Theme.LIGHT);
        assertThat(s.interfaceLanguage()).isEqualTo("en");
        assertThat(s.profileVisibility()).isEqualTo(ProfileSettings.Visibility.PUBLIC);
        assertThat(s.emailNotifications()).isFalse();
        assertThat(s.aiSuggestionsEnabled()).isFalse();
    }

    @Test
    void toSettingsDto_darkPrivate() {
        ProfileSettings s = new ProfileSettings(ProfileSettings.Theme.DARK, "ru",
                ProfileSettings.Visibility.PRIVATE, true, false);

        UserSettings dto = mapper.toSettingsDto(s);

        assertThat(dto.getTheme()).isEqualTo(UserSettings.ThemeEnum.DARK);
        assertThat(dto.getInterfaceLanguage()).isEqualTo("ru");
        assertThat(dto.getProfileVisibility()).isEqualTo(UserSettings.ProfileVisibilityEnum.PRIVATE);
        assertThat(dto.getEmailNotifications()).isTrue();
        assertThat(dto.getAiSuggestionsEnabled()).isFalse();
    }

    @Test
    void toSettingsDto_autoPublic_nullLanguage() {
        ProfileSettings s = new ProfileSettings(ProfileSettings.Theme.AUTO, null,
                ProfileSettings.Visibility.PUBLIC, false, true);

        UserSettings dto = mapper.toSettingsDto(s);

        assertThat(dto.getTheme()).isEqualTo(UserSettings.ThemeEnum.AUTO);
        assertThat(dto.getInterfaceLanguage()).isNull();
        assertThat(dto.getProfileVisibility()).isEqualTo(UserSettings.ProfileVisibilityEnum.PUBLIC);
        assertThat(dto.getEmailNotifications()).isFalse();
        assertThat(dto.getAiSuggestionsEnabled()).isTrue();
    }
}
