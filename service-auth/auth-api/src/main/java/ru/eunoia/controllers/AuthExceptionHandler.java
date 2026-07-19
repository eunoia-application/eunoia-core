package ru.eunoia.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.eunoia.application.domain.exception.AccountLockedException;
import ru.eunoia.application.domain.exception.AccountNotActiveException;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;

/**
 * Доменные исключения → HTTP-статусы. «Неверный пароль» и «нет юзера» отдаём одинаково (401),
 * чтобы не раскрывать, существует ли аккаунт.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({InvalidCredentialsException.class, UserNotFoundException.class})
    public ProblemDetail onInvalidCredentials(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(TokenValidationException.class)
    public ProblemDetail onInvalidToken(TokenValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler({AccountLockedException.class, AccountNotActiveException.class})
    public ProblemDetail onAccountBlocked(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail onUserExists(UserAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
