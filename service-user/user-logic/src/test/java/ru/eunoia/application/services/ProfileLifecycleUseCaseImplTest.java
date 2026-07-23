package ru.eunoia.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.domain.model.Profile;
import ru.eunoia.application.domain.model.ProfileSettings;
import ru.eunoia.application.port.out.ProfileRepositoryPort;

/** Жизненный цикл профиля: идемпотентное создание при регистрации и удаление аккаунта. */
@ExtendWith(MockitoExtension.class)
class ProfileLifecycleUseCaseImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "new@eunoia.ru";
    private static final String USERNAME = "newbie";

    @Mock
    private ProfileRepositoryPort profiles;

    private ProfileLifecycleUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProfileLifecycleUseCaseImpl(profiles);
    }

    @Test
    void onUserRegistered_newUser_createsAndSavesProfile() {
        when(profiles.existsByUserId(USER_ID)).thenReturn(false);

        useCase.onUserRegistered(USER_ID, EMAIL, USERNAME);

        ArgumentCaptor<Profile> created = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).existsByUserId(USER_ID);
        verify(profiles).save(created.capture());
        assertThat(created.getValue().userId()).isEqualTo(USER_ID);
        assertThat(created.getValue().email()).isEqualTo(EMAIL);
        assertThat(created.getValue().username()).isEqualTo(USERNAME);
        assertThat(created.getValue().emailVerified()).isFalse();
        assertThat(created.getValue().settings()).isEqualTo(ProfileSettings.defaults());
        verifyNoMoreInteractions(profiles);
    }

    @Test
    void onUserRegistered_existingProfile_isNoOp() {
        when(profiles.existsByUserId(USER_ID)).thenReturn(true);

        useCase.onUserRegistered(USER_ID, EMAIL, USERNAME);

        verify(profiles).existsByUserId(USER_ID);
        verify(profiles, never()).save(any());
        verifyNoMoreInteractions(profiles);
    }

    @Test
    void onUserDeleted_delegatesToRepository() {
        useCase.onUserDeleted(USER_ID);

        verify(profiles).deleteByUserId(USER_ID);
        verifyNoMoreInteractions(profiles);
    }
}
