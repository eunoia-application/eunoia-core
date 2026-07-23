package ru.eunoia.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.eunoia.application.domain.exception.ProfileNotFoundException;
import ru.eunoia.application.domain.exception.ProfilePrivateException;

/** Доменные исключения профиля → HTTP-статусы (ProblemDetail). */
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ProblemDetail onNotFound(ProfileNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProfilePrivateException.class)
    public ProblemDetail onPrivate(ProfilePrivateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
