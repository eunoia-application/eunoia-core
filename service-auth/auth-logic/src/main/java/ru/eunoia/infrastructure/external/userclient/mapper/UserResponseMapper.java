package ru.eunoia.infrastructure.external.userclient.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.eunoia.domain.model.User;
import ru.eunoia.infrastructure.external.userclient.dto.UserResponseDto;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "emailVerified", target = "emailVerified")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "lastLoginAt", target = "lastLoginAt")
    @Mapping(source = "failedLoginAttempts", target = "failedLoginAttempts")
    @Mapping(source = "lockedUntil", target = "lockedUntil")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "settings", ignore = true)
    User toDomain(UserResponseDto dto);

}
