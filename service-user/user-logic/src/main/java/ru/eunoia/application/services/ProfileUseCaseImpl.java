package ru.eunoia.application.services;

import java.util.Set;
import java.util.UUID;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.exception.AvatarNotFoundException;
import ru.eunoia.application.domain.exception.InvalidAvatarException;
import ru.eunoia.application.domain.exception.ProfileNotFoundException;
import ru.eunoia.application.domain.exception.ProfilePrivateException;
import ru.eunoia.application.domain.model.Avatar;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.in.ProfileUseCase;
import ru.eunoia.application.port.out.AvatarStoragePort;
import ru.eunoia.application.port.out.ProfileRepositoryPort;

/** Чтение и правка профиля. Все изменения — через доменные with-методы (профиль неизменяем). */
public class ProfileUseCaseImpl implements ProfileUseCase {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final ProfileRepositoryPort profiles;
    private final AvatarStoragePort avatars;

    public ProfileUseCaseImpl(ProfileRepositoryPort profiles, AvatarStoragePort avatars) {
        this.profiles = profiles;
        this.avatars = avatars;
    }

    @Override
    public Profile getMyProfile(UUID userId) {
        return require(userId);
    }

    @Override
    public Profile getPublicProfile(UUID userId) {
        Profile profile = require(userId);
        if (!profile.isPublic()) {
            throw new ProfilePrivateException(userId.toString());
        }
        return profile;
    }

    @Override
    public Profile updateProfile(UUID userId, UpdateProfileCommand command) {
        return profiles.save(require(userId)
                .withProfile(command.firstName(), command.lastName(), command.bio(), command.avatarUrl()));
    }

    @Override
    public Profile updateSettings(UUID userId, ProfileSettings settings) {
        return profiles.save(require(userId).withSettings(settings));
    }

    @Override
    public Profile setAvatar(UUID userId, String avatarUrl) {
        return profiles.save(require(userId).withAvatar(avatarUrl));
    }

    @Override
    public Profile removeAvatar(UUID userId) {
        Profile profile = require(userId);
        avatars.delete(userId);
        return profiles.save(profile.withAvatar(null));
    }

    @Override
    public Profile uploadAvatar(UUID userId, Avatar avatar) {
        if (avatar.content() == null || avatar.content().length == 0) {
            throw new InvalidAvatarException("Пустой файл аватара");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(avatar.contentType())) {
            throw new InvalidAvatarException("Недопустимый тип аватара: " + avatar.contentType());
        }
        Profile profile = require(userId);
        avatars.store(userId, avatar);
        return profiles.save(profile.withAvatar("/users/" + userId + "/avatar"));
    }

    @Override
    public Avatar getAvatar(UUID userId) {
        return avatars.load(userId).orElseThrow(() -> new AvatarNotFoundException(userId));
    }

    private Profile require(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId.toString()));
    }
}
