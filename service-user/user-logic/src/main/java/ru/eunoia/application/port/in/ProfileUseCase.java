package ru.eunoia.application.port.in;

import java.util.UUID;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.model.Avatar;
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

    /** Загрузить файл-аватар: валидирует, сохраняет байты, ставит avatarUrl на отдачу (/users/{id}/avatar). */
    Profile uploadAvatar(UUID userId, Avatar avatar);

    /** Байты аватара для отдачи (GET /users/{id}/avatar). */
    Avatar getAvatar(UUID userId);
}
