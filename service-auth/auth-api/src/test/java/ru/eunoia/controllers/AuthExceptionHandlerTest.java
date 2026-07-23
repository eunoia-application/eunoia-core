package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;

/**
 * Доменные исключения → HTTP-статусы. Каждый @ExceptionHandler проверяем отдельно.
 */
class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void invalidCredentials_mapsTo401_withGenericDetail() {
        ProblemDetail problem = handler.onInvalidCredentials(new InvalidCredentialsException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid email or password");
    }

    @Test
    void userNotFound_mapsTo401_withSameGenericDetail() {
        // Тот же обработчик: «нет юзера» и «неверный пароль» не различаем наружу.
        ProblemDetail problem = handler.onInvalidCredentials(new UserNotFoundException("no such user"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid email or password");
    }

    @Test
    void invalidToken_mapsTo401_carryingExceptionMessage() {
        ProblemDetail problem = handler.onInvalidToken(new TokenValidationException("token expired"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo("token expired");
    }

    @Test
    void accountLocked_mapsTo403() {
        ProblemDetail problem = handler.onAccountBlocked(new AccountLockedException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        // AccountLockedException не несёт сообщения — detail берётся из getMessage() и равен null.
        assertThat(problem.getDetail()).isNull();
    }

    @Test
    void accountNotActive_mapsTo403() {
        ProblemDetail problem = handler.onAccountBlocked(new AccountNotActiveException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getDetail()).isNull();
    }

    @Test
    void userAlreadyExists_mapsTo409_carryingExceptionMessage() {
        ProblemDetail problem = handler.onUserExists(new UserAlreadyExistsException("email already taken"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo("email already taken");
    }
}
