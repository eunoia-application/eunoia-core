package ru.eunoia.application.port.in;

import java.util.UUID;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;

/** Операции пользователя над своим профилем + чтение публичного профиля. */
public interface ProfileUseCase {

    Profile getMyProfile(UUID userId);

    Profile getPublicProfile(UUID userId);

    Profile updateProfile(UUID userId, UpdateProfileCommand command);

    Profile updateSettings(UUID userId, ProfileSettings settings);

    Profile setAvatar(UUID userId, String avatarUrl);

    Profile removeAvatar(UUID userId);
}
