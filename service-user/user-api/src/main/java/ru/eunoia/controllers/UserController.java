package ru.eunoia.controllers;

import com.eunoia.application.user.api.UsersApi;
import com.eunoia.application.user.model.UserDataExport;
import com.eunoia.application.user.model.UserProfile;
import com.eunoia.application.user.model.UserPublicProfile;
import com.eunoia.application.user.model.UserSettings;
import com.eunoia.application.user.model.UserUpdateRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.port.in.ProfileUseCase;
import ru.eunoia.mappers.UserApiMapper;
import ru.eunoia.security.CurrentUser;

/** REST-адаптер профиля: реализует сгенерированный из контракта {@code UsersApi}. */
@RestController
public class UserController implements UsersApi {

    private final ProfileUseCase profiles;
    private final CurrentUser currentUser;
    private final UserApiMapper mapper;

    public UserController(ProfileUseCase profiles, CurrentUser currentUser, UserApiMapper mapper) {
        this.profiles = profiles;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<UserProfile> getCurrentUser() {
        return ResponseEntity.ok(mapper.toProfile(profiles.getMyProfile(currentUser.id())));
    }

    @Override
    public ResponseEntity<UserProfile> updateCurrentUser(UserUpdateRequest request) {
        Profile updated = profiles.updateProfile(currentUser.id(), mapper.toCommand(request));
        return ResponseEntity.ok(mapper.toProfile(updated));
    }

    @Override
    public ResponseEntity<UserProfile> updateSettings(UserSettings userSettings) {
        return ResponseEntity.ok(mapper.toProfile(
                profiles.updateSettings(currentUser.id(), mapper.toSettings(userSettings))));
    }

    @Override
    public ResponseEntity<UserProfile> deleteAvatar() {
        return ResponseEntity.ok(mapper.toProfile(profiles.removeAvatar(currentUser.id())));
    }

    @Override
    public ResponseEntity<UserProfile> uploadAvatar(MultipartFile file) {
        // TODO M3+: залить файл в блоб-хранилище (S3/MinIO) и проставить avatarUrl.
        // Пока URL аватара задаётся напрямую через updateCurrentUser (avatarUrl).
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<UserDataExport> exportMyData() {
        return ResponseEntity.ok(mapper.toExport(profiles.getMyProfile(currentUser.id())));
    }

    @Override
    public ResponseEntity<UserPublicProfile> getUserProfile(UUID userId) {
        return ResponseEntity.ok(mapper.toPublicProfile(profiles.getPublicProfile(userId)));
    }
}
