package ru.eunoia.application.comand;

public record RegisterCommand(
        String email,
        String password
) {}
