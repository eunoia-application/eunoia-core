package ru.eunoia.application.services;

import java.util.UUID;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.exception.ProfileNotFoundException;
import ru.eunoia.application.domain.exception.ProfilePrivateException;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.in.ProfileUseCase;
import ru.eunoia.application.port.out.ProfileRepositoryPort;

/** Чтение и правка профиля. Все изменения — через доменные with-методы (профиль неизменяем). */
public class ProfileUseCaseImpl implements ProfileUseCase {

    private final ProfileRepositoryPort profiles;

    public ProfileUseCaseImpl(ProfileRepositoryPort profiles) {
        this.profiles = profiles;
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
        return profiles.save(require(userId).withAvatar(null));
    }

    private Profile require(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId.toString()));
    }
}
