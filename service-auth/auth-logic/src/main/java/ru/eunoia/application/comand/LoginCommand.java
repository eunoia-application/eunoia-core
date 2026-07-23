package ru.eunoia.application.comand;

public record LoginCommand(
        String email,
        String password
) {}
