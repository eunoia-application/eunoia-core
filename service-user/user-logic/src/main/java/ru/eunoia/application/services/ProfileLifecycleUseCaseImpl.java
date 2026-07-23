package ru.eunoia.application.services;

import java.util.UUID;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.port.in.ProfileLifecycleUseCase;
import ru.eunoia.application.port.out.ProfileRepositoryPort;

/** Профиль следует за аккаунтом в auth. Идемпотентно — Kafka может доставить событие повторно. */
public class ProfileLifecycleUseCaseImpl implements ProfileLifecycleUseCase {

    private final ProfileRepositoryPort profiles;

    public ProfileLifecycleUseCaseImpl(ProfileRepositoryPort profiles) {
        this.profiles = profiles;
    }

    @Override
    public void onUserRegistered(UUID userId, String email, String username) {
        if (!profiles.existsByUserId(userId)) {
            profiles.save(Profile.createFor(userId, email, username));
        }
    }

    @Override
    public void onUserDeleted(UUID userId) {
        profiles.deleteByUserId(userId);
    }
}
