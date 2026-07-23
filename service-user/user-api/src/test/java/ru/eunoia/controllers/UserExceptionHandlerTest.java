package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import ru.eunoia.application.domain.exception.AvatarNotFoundException;
import ru.eunoia.application.domain.exception.InvalidAvatarException;
import ru.eunoia.application.domain.exception.ProfileNotFoundException;
import ru.eunoia.application.domain.exception.ProfilePrivateException;

/**
 * Доменные исключения профиля → HTTP-статусы. Каждый @ExceptionHandler вызываем напрямую
 * и сверяем статус и detail (сообщение исключения) в ProblemDetail.
 */
class UserExceptionHandlerTest {

    private final UserExceptionHandler handler = new UserExceptionHandler();

    @Test
    void profileNotFound_mapsTo404_carryingExceptionMessage() {
        String userId = UUID.randomUUID().toString();

        ProblemDetail problem = handler.onNotFound(new ProfileNotFoundException(userId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("Профиль не найден: " + userId);
    }

    @Test
    void profilePrivate_mapsTo403_carryingExceptionMessage() {
        String userId = UUID.randomUUID().toString();

        ProblemDetail problem = handler.onPrivate(new ProfilePrivateException(userId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getDetail()).isEqualTo("Профиль скрыт: " + userId);
    }

    @Test
    void avatarNotFound_mapsTo404_carryingExceptionMessage() {
        UUID userId = UUID.randomUUID();

        ProblemDetail problem = handler.onAvatarNotFound(new AvatarNotFoundException(userId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("Аватар не найден: " + userId);
    }

    @Test
    void invalidAvatar_mapsTo400_carryingExceptionMessage() {
        ProblemDetail problem = handler.onInvalidAvatar(new InvalidAvatarException("Пустой файл аватара"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Пустой файл аватара");
    }
}
