package ru.eunoia.application.port.in;

import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.model.Authentication;

public interface LoginUseCase {

    Authentication login(LoginCommand command);
}
