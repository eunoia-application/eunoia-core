package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunoia.application.user.model.UserDataExport;
import com.eunoia.application.user.model.UserProfile;
import com.eunoia.application.user.model.UserPublicProfile;
import com.eunoia.application.user.model.UserSettings;
import com.eunoia.application.user.model.UserUpdateRequest;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.exception.InvalidAvatarException;
import ru.eunoia.application.domain.model.Avatar;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.in.ProfileUseCase;
import ru.eunoia.mappers.UserApiMapper;
import ru.eunoia.security.CurrentUser;

/**
 * Тонкий веб-адаптер профиля: проверяем, что каждый endpoint делегирует в ProfileUseCase
 * (по currentUser.id() для «своих» операций) и возвращает нужный HTTP-статус с замапленным
 * телом. Маппер замокан — логика преобразования проверяется в UserApiMapperTest.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ProfileUseCase profiles;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private UserApiMapper mapper;

    @InjectMocks
    private UserController controller;

    private static Profile sampleProfile() {
        return Profile.createFor(USER_ID, "user@example.com", "eunoia_user");
    }

    @Test
    void getCurrentUser_usesCurrentUserId_andReturns200WithMappedBody() {
        Profile profile = sampleProfile();
        UserProfile mapped = new UserProfile();
        when(currentUser.id()).thenReturn(USER_ID);
        when(profiles.getMyProfile(USER_ID)).thenReturn(profile);
        when(mapper.toProfile(profile)).thenReturn(mapped);

        ResponseEntity<UserProfile> response = controller.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(currentUser).id();
        verify(profiles).getMyProfile(USER_ID);
        verify(mapper).toProfile(profile);
    }

    @Test
    void updateCurrentUser_mapsRequestToCommand_delegates_andReturns200() {
        UserUpdateRequest request = new UserUpdateRequest();
        UpdateProfileCommand command = new UpdateProfileCommand("Alex", "Johnson", "bio", null);
        Profile updated = sampleProfile();
        UserProfile mapped = new UserProfile();
        when(currentUser.id()).thenReturn(USER_ID);
        when(mapper.toCommand(request)).thenReturn(command);
        when(profiles.updateProfile(USER_ID, command)).thenReturn(updated);
        when(mapper.toProfile(updated)).thenReturn(mapped);

        ResponseEntity<UserProfile> response = controller.updateCurrentUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(mapper).toCommand(request);
        verify(profiles).updateProfile(USER_ID, command);
        verify(mapper).toProfile(updated);
    }

    @Test
    void updateSettings_mapsSettings_delegates_andReturns200() {
        UserSettings settingsDto = new UserSettings();
        ProfileSettings settings = ProfileSettings.defaults();
        Profile updated = sampleProfile();
        UserProfile mapped = new UserProfile();
        when(currentUser.id()).thenReturn(USER_ID);
        when(mapper.toSettings(settingsDto)).thenReturn(settings);
        when(profiles.updateSettings(USER_ID, settings)).thenReturn(updated);
        when(mapper.toProfile(updated)).thenReturn(mapped);

        ResponseEntity<UserProfile> response = controller.updateSettings(settingsDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(mapper).toSettings(settingsDto);
        verify(profiles).updateSettings(USER_ID, settings);
        verify(mapper).toProfile(updated);
    }

    @Test
    void deleteAvatar_delegatesToRemoveAvatar_andReturns200() {
        Profile updated = sampleProfile();
        UserProfile mapped = new UserProfile();
        when(currentUser.id()).thenReturn(USER_ID);
        when(profiles.removeAvatar(USER_ID)).thenReturn(updated);
        when(mapper.toProfile(updated)).thenReturn(mapped);

        ResponseEntity<UserProfile> response = controller.deleteAvatar();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(profiles).removeAvatar(USER_ID);
        verify(mapper).toProfile(updated);
    }

    @Test
    void exportMyData_readsMyProfile_andReturns200WithMappedExport() {
        Profile profile = sampleProfile();
        UserDataExport mapped = new UserDataExport();
        when(currentUser.id()).thenReturn(USER_ID);
        when(profiles.getMyProfile(USER_ID)).thenReturn(profile);
        when(mapper.toExport(profile)).thenReturn(mapped);

        ResponseEntity<UserDataExport> response = controller.exportMyData();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(profiles).getMyProfile(USER_ID);
        verify(mapper).toExport(profile);
    }

    @Test
    void getUserProfile_readsPublicProfileById_andReturns200() {
        UUID targetId = UUID.randomUUID();
        Profile profile = sampleProfile();
        UserPublicProfile mapped = new UserPublicProfile();
        when(profiles.getPublicProfile(targetId)).thenReturn(profile);
        when(mapper.toPublicProfile(profile)).thenReturn(mapped);

        ResponseEntity<UserPublicProfile> response = controller.getUserProfile(targetId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        verify(profiles).getPublicProfile(targetId);
        verify(mapper).toPublicProfile(profile);
    }

    @Test
    void uploadAvatar_readsFileBytesIntoAvatar_delegates_andReturns200() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        MultipartFile file = mock(MultipartFile.class);
        Profile updated = sampleProfile();
        UserProfile mapped = new UserProfile();
        when(currentUser.id()).thenReturn(USER_ID);
        when(file.getBytes()).thenReturn(bytes);
        when(file.getContentType()).thenReturn("image/png");
        when(profiles.uploadAvatar(eq(USER_ID), any(Avatar.class))).thenReturn(updated);
        when(mapper.toProfile(updated)).thenReturn(mapped);

        ResponseEntity<UserProfile> response = controller.uploadAvatar(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mapped);
        // байты и MIME из multipart уезжают в use case доменным Avatar
        ArgumentCaptor<Avatar> avatar = ArgumentCaptor.forClass(Avatar.class);
        verify(profiles).uploadAvatar(eq(USER_ID), avatar.capture());
        assertThat(avatar.getValue().content()).isEqualTo(bytes);
        assertThat(avatar.getValue().contentType()).isEqualTo("image/png");
        verify(mapper).toProfile(updated);
    }

    @Test
    void uploadAvatar_fileReadFails_throwsInvalidAvatar() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        // сбой чтения multipart трактуем как невалидный файл (400), а не 500
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(InvalidAvatarException.class);
    }

    @Test
    void getUserAvatar_returnsBytesAsResource_withDeclaredContentType_and200() {
        UUID targetId = UUID.randomUUID();
        byte[] bytes = {10, 20, 30};
        when(profiles.getAvatar(targetId)).thenReturn(new Avatar(bytes, "image/png"));

        ResponseEntity<Resource> response = controller.getUserAvatar(targetId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).isInstanceOf(ByteArrayResource.class);
        assertThat(((ByteArrayResource) response.getBody()).getByteArray()).isEqualTo(bytes);
        verify(profiles).getAvatar(targetId);
    }
}
