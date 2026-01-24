package ru.eunoia.mappers;

import com.eunoia.application.auth.model.AuthResponse;
import com.eunoia.application.auth.model.LoginRequest;
import com.eunoia.application.auth.model.RefreshTokenRequest;
import com.eunoia.application.auth.model.RegisterRequest;
import com.eunoia.application.auth.model.UserProfile;
import com.eunoia.application.auth.model.UserPublicProfile;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.domain.model.UserSettings;
import ru.eunoia.application.domain.model.UserStats;
import ru.eunoia.application.domain.model.enums.DefaultNoteStatus;
import ru.eunoia.application.domain.model.enums.Theme;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuthMapper {

    // =========== DTO -> Domain ===========

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "locked", constant = "false")
    @Mapping(target = "stats", expression = "java(getEmptyStats())")
    @Mapping(target = "settings", expression = "java(getDefaultSettings())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "lockedUntil", ignore = true)
    User toDomain(RegisterRequest dto);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    User toDomain(LoginRequest dto);

    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "tokenHash", expression = "java(hashToken(dto.getRefreshToken()))")
    @Mapping(target = "deviceInfo", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "userAgent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "usedAt", ignore = true)
    @Mapping(target = "revoked", constant = "false")
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revocationReason", ignore = true)
    RefreshToken toDomain(RefreshTokenRequest dto);

    // =========== Domain -> DTO ===========

    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "refreshToken", target = "refreshToken")
    @Mapping(source = "tokenType", target = "tokenType")
    @Mapping(source = "expiresIn", target = "expiresIn")
    @Mapping(target = "user", ignore = true) // заполняется отдельно
    AuthResponse toDto(AuthTokens authTokens);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "emailVerified", target = "emailVerified")
    @Mapping(source = "stats", target = "stats")
    @Mapping(source = "settings", target = "settings")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    UserProfile toDto(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "stats", target = "stats")
    @Mapping(source = "createdAt", target = "createdAt")
    UserPublicProfile toPublicDto(User user);

    // =========== Комбинированные методы ===========

    default AuthResponse toAuthResponseDto(AuthTokens authTokens, User user) {
        AuthResponse dto = toDto(authTokens);
        dto.setUser(toDto(user));
        return dto;
    }

    // =========== Вспомогательные методы ===========

    default UserStats getEmptyStats() {
        return UserStats.builder()
                .noteCount(0)
                .tagCount(0)
                .evergreenNoteCount(0)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    default UserSettings getDefaultSettings() {
        return UserSettings.builder()
                .theme(Theme.AUTO)
                .defaultNoteStatus(DefaultNoteStatus.DRAFT)
                .emailNotifications(true)
                .aiSuggestionsEnabled(true)
                .build();
    }

    default String hashToken(String token) {
        // В реальности здесь хеширование токена
        return "hashed_" + token;
    }

    @Named("mapThemeToString")
    default String mapThemeToString(Theme theme) {
        return theme != null ? theme.name() : "AUTO";
    }

    @Named("mapStringToTheme")
    default Theme mapStringToTheme(String theme) {
        if (theme == null) return Theme.AUTO;
        try {
            return Theme.valueOf(theme.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Theme.AUTO;
        }
    }

    @Named("mapStatusToString")
    default String mapStatusToString(DefaultNoteStatus status) {
        return status != null ? status.name() : "DRAFT";
    }

    @Named("mapStringToStatus")
    default DefaultNoteStatus mapStringToStatus(String status) {
        if (status == null) return DefaultNoteStatus.DRAFT;
        try {
            return DefaultNoteStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DefaultNoteStatus.DRAFT;
        }
    }

}
