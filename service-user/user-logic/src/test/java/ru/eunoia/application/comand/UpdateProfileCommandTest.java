package ru.eunoia.application.comand;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Команда обновления профиля: аксессоры записи. */
class UpdateProfileCommandTest {

    @Test
    void accessors_returnConstructorValues() {
        UpdateProfileCommand cmd = new UpdateProfileCommand("First", "Last", "bio", "http://ava");

        assertThat(cmd.firstName()).isEqualTo("First");
        assertThat(cmd.lastName()).isEqualTo("Last");
        assertThat(cmd.bio()).isEqualTo("bio");
        assertThat(cmd.avatarUrl()).isEqualTo("http://ava");
    }

    @Test
    void allFieldsOptional_acceptsNulls() {
        UpdateProfileCommand cmd = new UpdateProfileCommand(null, null, null, null);

        assertThat(cmd.firstName()).isNull();
        assertThat(cmd.lastName()).isNull();
        assertThat(cmd.bio()).isNull();
        assertThat(cmd.avatarUrl()).isNull();
    }
}
