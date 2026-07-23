package ru.eunoia.mappers;

import com.eunoia.application.user.model.UserDataExport;
import com.eunoia.application.user.model.UserProfile;
import com.eunoia.application.user.model.UserPublicProfile;
import com.eunoia.application.user.model.UserSettings;
import com.eunoia.application.user.model.UserUpdateRequest;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;

/** Перевод между доменным Profile и DTO контракта. Имена enum-констант совпадают (LIGHT/DARK/AUTO, PUBLIC/PRIVATE). */
@Component
public class UserApiMapper {

    /** Публичная база API (через gateway), чтобы avatarUrl загруженного файла был готов для <img src>. */
    private final String publicBase;

    public UserApiMapper(@Value("${app.public-url}") String publicBase) {
        this.publicBase = publicBase;
    }

    public UserProfile toProfile(Profile p) {
        UserProfile dto = new UserProfile();
        dto.setId(p.userId());
        dto.setEmail(p.email());
        dto.setUsername(p.username());
        dto.setFirstName(p.firstName());
        dto.setLastName(p.lastName());
        dto.setAvatarUrl(toUri(p.avatarUrl()));
        dto.setBio(p.bio());
        dto.setEmailVerified(p.emailVerified());
        dto.setSettings(toSettingsDto(p.settings()));
        dto.setCreatedAt(p.createdAt());
        dto.setUpdatedAt(p.updatedAt());
        return dto;
    }

    public UserPublicProfile toPublicProfile(Profile p) {
        UserPublicProfile dto = new UserPublicProfile();
        dto.setId(p.userId());
        dto.setUsername(p.username());
        dto.setFirstName(p.firstName());
        dto.setLastName(p.lastName());
        dto.setAvatarUrl(toUri(p.avatarUrl()));
        dto.setBio(p.bio());
        dto.setCreatedAt(p.createdAt());
        return dto;
    }

    public UserDataExport toExport(Profile p) {
        UserDataExport dto = new UserDataExport();
        dto.setExportedAt(LocalDateTime.now());
        dto.setProfile(toProfile(p));
        return dto;
    }

    public UpdateProfileCommand toCommand(UserUpdateRequest req) {
        String avatarUrl = req.getAvatarUrl() == null ? null : req.getAvatarUrl().toString();
        return new UpdateProfileCommand(req.getFirstName(), req.getLastName(), req.getBio(), avatarUrl);
    }

    /**
     * Домен хранит URL строкой; контракт — java.net.URI. Загруженный аватар лежит относительным
     * путём (/users/{id}/avatar) — достраиваем до публичного адреса gateway, чтобы <img src> работал
     * напрямую. Внешний URL (задан пользователем) отдаём как есть. Null пробрасываем.
     */
    private URI toUri(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value.startsWith("/") ? publicBase + value : value);
    }

    public ProfileSettings toSettings(UserSettings dto) {
        return new ProfileSettings(
                dto.getTheme() == null ? ProfileSettings.Theme.AUTO
                        : ProfileSettings.Theme.valueOf(dto.getTheme().name()),
                dto.getInterfaceLanguage(),
                dto.getProfileVisibility() == null ? ProfileSettings.Visibility.PRIVATE
                        : ProfileSettings.Visibility.valueOf(dto.getProfileVisibility().name()),
                dto.getEmailNotifications() == null || dto.getEmailNotifications(),
                dto.getAiSuggestionsEnabled() == null || dto.getAiSuggestionsEnabled());
    }

    public UserSettings toSettingsDto(ProfileSettings s) {
        UserSettings dto = new UserSettings();
        dto.setTheme(UserSettings.ThemeEnum.valueOf(s.theme().name()));
        dto.setInterfaceLanguage(s.interfaceLanguage());
        dto.setProfileVisibility(UserSettings.ProfileVisibilityEnum.valueOf(s.profileVisibility().name()));
        dto.setEmailNotifications(s.emailNotifications());
        dto.setAiSuggestionsEnabled(s.aiSuggestionsEnabled());
        return dto;
    }
}
