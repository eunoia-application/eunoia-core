package ru.eunoia.mappers;

import com.eunoia.application.auth.model.ChangePasswordRequest;
import com.eunoia.application.auth.model.UserPublicStats;
import com.eunoia.application.auth.model.UserSettings;
import com.eunoia.application.auth.model.UserUpdateRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.domain.model.UserStats;

@Mapper(
        componentModel = "spring",
        uses = {AuthMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    // =========== UserStats Domain <-> DTO ===========

    @Mapping(source = "noteCount", target = "noteCount")
    @Mapping(source = "tagCount", target = "tagCount")
    @Mapping(source = "evergreenNoteCount", target = "evergreenNoteCount")
    @Mapping(source = "lastActiveAt", target = "lastActiveAt")
    UserStats toDto(UserStats stats);

    @Mapping(source = "noteCount", target = "noteCount")
    @Mapping(source = "tagCount", target = "tagCount")
    @Mapping(source = "evergreenNoteCount", target = "evergreenNoteCount")
    @Mapping(source = "lastActiveAt", target = "lastActiveAt")
    UserStats toDomain(UserStats dto);

    // =========== UserPublicStats Domain <-> DTO ===========

    @Mapping(source = "noteCount", target = "noteCount")
    @Mapping(source = "evergreenNoteCount", target = "evergreenNoteCount")
    UserPublicStats toDto(ru.eunoia.application.domain.model.UserPublicStats stats);

    @Mapping(source = "noteCount", target = "noteCount")
    @Mapping(source = "evergreenNoteCount", target = "evergreenNoteCount")
    ru.eunoia.application.domain.model.UserPublicStats toDomain(UserPublicStats dto);

    // =========== UserSettings Domain <-> DTO ===========

    @Mapping(target = "theme", qualifiedByName = "mapThemeToString")
    @Mapping(target = "defaultNoteStatus", qualifiedByName = "mapStatusToString")
    @Mapping(source = "emailNotifications", target = "emailNotifications")
    @Mapping(source = "aiSuggestionsEnabled", target = "aiSuggestionsEnabled")
    UserSettings toDto(ru.eunoia.application.domain.model.UserSettings settings);

    @Mapping(target = "theme", qualifiedByName = "mapStringToTheme")
    @Mapping(target = "defaultNoteStatus", qualifiedByName = "mapStringToStatus")
    @Mapping(source = "emailNotifications", target = "emailNotifications")
    @Mapping(source = "aiSuggestionsEnabled", target = "aiSuggestionsEnabled")
    ru.eunoia.application.domain.model.UserSettings toDomain(UserSettings dto);

    // =========== UserUpdateRequest -> User (частичное обновление) ===========

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    User toDomain(UserUpdateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    void updateUserFromDto(UserUpdateRequest dto, @MappingTarget User user);

    // =========== ChangePasswordRequest -> Domain объект ===========

    default PasswordChange toPasswordChange(ChangePasswordRequest dto, UUID userId) {
        return PasswordChange.builder()
                .userId(userId)
                .currentPassword(dto.getCurrentPassword())
                .newPassword(dto.getNewPassword())
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // =========== Вспомогательный класс для смены пароля ===========

    @Builder
    @Getter
    class PasswordChange {
        private UUID userId;
        private String currentPassword;
        private String newPassword;
        private LocalDateTime requestedAt;
    }

}
