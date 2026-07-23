package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.comand.UpdateProfileCommand;
import ru.eunoia.application.domain.exception.AvatarNotFoundException;
import ru.eunoia.application.domain.exception.InvalidAvatarException;
import ru.eunoia.application.domain.exception.ProfileNotFoundException;
import ru.eunoia.application.domain.exception.ProfilePrivateException;
import ru.eunoia.application.domain.model.Avatar;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.out.AvatarStoragePort;
import ru.eunoia.application.port.out.ProfileRepositoryPort;

/** Чтение и правка профиля: найденный/не найденный профиль и приватность. */
@ExtendWith(MockitoExtension.class)
class ProfileUseCaseImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProfileRepositoryPort profiles;
    @Mock
    private AvatarStoragePort avatars;

    private ProfileUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProfileUseCaseImpl(profiles, avatars);
    }

    /** Профиль с заданными настройками (они определяют публичность). */
    private static Profile profileWith(ProfileSettings settings) {
        LocalDateTime now = LocalDateTime.now();
        return new Profile(USER_ID, "u@eunoia.ru", "user", "First", "Last",
                "http://ava", "bio", true, settings, now, now);
    }

    private static Profile privateProfile() {
        return profileWith(ProfileSettings.defaults());
    }

    private static Profile publicProfile() {
        return profileWith(new ProfileSettings(
                ProfileSettings.Theme.AUTO, null, ProfileSettings.Visibility.PUBLIC, true, true));
    }

    @Test
    void getMyProfile_found_returnsProfile() {
        Profile stored = privateProfile();
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        assertThat(useCase.getMyProfile(USER_ID)).isSameAs(stored);
        verify(profiles, never()).save(any());
    }

    @Test
    void getMyProfile_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getMyProfile(USER_ID))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void getPublicProfile_public_returnsProfile() {
        Profile stored = publicProfile();
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        assertThat(useCase.getPublicProfile(USER_ID)).isSameAs(stored);
        verify(profiles, never()).save(any());
    }

    @Test
    void getPublicProfile_private_throwsPrivate() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));

        assertThatThrownBy(() -> useCase.getPublicProfile(USER_ID))
                .isInstanceOf(ProfilePrivateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void getPublicProfile_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getPublicProfile(USER_ID))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void updateProfile_found_savesTransformedProfile() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        Profile persisted = publicProfile();
        when(profiles.save(any(Profile.class))).thenReturn(persisted);

        Profile result = useCase.updateProfile(USER_ID,
                new UpdateProfileCommand("Neo", "Anderson", "hacker", "http://new"));

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<Profile> toSave = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(toSave.capture());
        assertThat(toSave.getValue().userId()).isEqualTo(USER_ID);
        assertThat(toSave.getValue().firstName()).isEqualTo("Neo");
        assertThat(toSave.getValue().lastName()).isEqualTo("Anderson");
        assertThat(toSave.getValue().bio()).isEqualTo("hacker");
        assertThat(toSave.getValue().avatarUrl()).isEqualTo("http://new");
    }

    @Test
    void updateProfile_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateProfile(USER_ID,
                new UpdateProfileCommand("Neo", "Anderson", "hacker", "http://new")))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void updateSettings_found_savesTransformedProfile() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        Profile persisted = publicProfile();
        when(profiles.save(any(Profile.class))).thenReturn(persisted);
        ProfileSettings newSettings = new ProfileSettings(
                ProfileSettings.Theme.DARK, "ru", ProfileSettings.Visibility.PUBLIC, false, false);

        Profile result = useCase.updateSettings(USER_ID, newSettings);

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<Profile> toSave = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(toSave.capture());
        assertThat(toSave.getValue().settings()).isSameAs(newSettings);
    }

    @Test
    void updateSettings_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateSettings(USER_ID, ProfileSettings.defaults()))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void setAvatar_found_savesTransformedProfile() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        Profile persisted = privateProfile();
        when(profiles.save(any(Profile.class))).thenReturn(persisted);

        Profile result = useCase.setAvatar(USER_ID, "http://new-avatar");

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<Profile> toSave = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(toSave.capture());
        assertThat(toSave.getValue().avatarUrl()).isEqualTo("http://new-avatar");
    }

    @Test
    void setAvatar_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.setAvatar(USER_ID, "http://new-avatar"))
                .isInstanceOf(ProfileNotFoundException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void removeAvatar_found_savesProfileWithoutAvatar() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        Profile persisted = privateProfile();
        when(profiles.save(any(Profile.class))).thenReturn(persisted);

        Profile result = useCase.removeAvatar(USER_ID);

        assertThat(result).isSameAs(persisted);
        // байты аватара тоже стираем из хранилища, не только url в профиле
        verify(avatars).delete(USER_ID);
        ArgumentCaptor<Profile> toSave = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(toSave.capture());
        assertThat(toSave.getValue().avatarUrl()).isNull();
    }

    @Test
    void removeAvatar_missing_throwsNotFound() {
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.removeAvatar(USER_ID))
                .isInstanceOf(ProfileNotFoundException.class);
        // профиля нет → ни байты не трогаем, ни профиль не сохраняем
        verify(avatars, never()).delete(any());
        verify(profiles, never()).save(any());
    }

    @Test
    void uploadAvatar_validPng_storesBytesAndSavesProfileWithServedUrl() {
        Avatar avatar = new Avatar(new byte[] {1, 2, 3}, "image/png");
        when(profiles.findByUserId(USER_ID)).thenReturn(Optional.of(privateProfile()));
        Profile persisted = privateProfile();
        when(profiles.save(any(Profile.class))).thenReturn(persisted);

        Profile result = useCase.uploadAvatar(USER_ID, avatar);

        assertThat(result).isSameAs(persisted);
        verify(avatars).store(USER_ID, avatar);
        // профилю проставляется url на публичную отдачу /users/{id}/avatar
        ArgumentCaptor<Profile> toSave = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(toSave.capture());
        assertThat(toSave.getValue().avatarUrl()).isEqualTo("/users/" + USER_ID + "/avatar");
    }

    @Test
    void uploadAvatar_unsupportedType_throwsInvalid_andTouchesNothing() {
        Avatar avatar = new Avatar(new byte[] {1, 2, 3}, "image/gif");

        assertThatThrownBy(() -> useCase.uploadAvatar(USER_ID, avatar))
                .isInstanceOf(InvalidAvatarException.class);
        // тип отсеивается до обращения к профилю и хранилищу
        verify(avatars, never()).store(any(), any());
        verify(profiles, never()).save(any());
    }

    @Test
    void uploadAvatar_emptyContent_throwsInvalid_andTouchesNothing() {
        Avatar avatar = new Avatar(new byte[0], "image/png");

        assertThatThrownBy(() -> useCase.uploadAvatar(USER_ID, avatar))
                .isInstanceOf(InvalidAvatarException.class);
        verify(avatars, never()).store(any(), any());
        verify(profiles, never()).save(any());
    }

    @Test
    void uploadAvatar_nullContent_throwsInvalid_andTouchesNothing() {
        Avatar avatar = new Avatar(null, "image/png");

        assertThatThrownBy(() -> useCase.uploadAvatar(USER_ID, avatar))
                .isInstanceOf(InvalidAvatarException.class);
        verify(avatars, never()).store(any(), any());
        verify(profiles, never()).save(any());
    }

    @Test
    void getAvatar_found_returnsAvatar() {
        Avatar stored = new Avatar(new byte[] {9, 8, 7}, "image/webp");
        when(avatars.load(USER_ID)).thenReturn(Optional.of(stored));

        assertThat(useCase.getAvatar(USER_ID)).isSameAs(stored);
    }

    @Test
    void getAvatar_missing_throwsNotFound() {
        when(avatars.load(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAvatar(USER_ID))
                .isInstanceOf(AvatarNotFoundException.class);
    }
}
