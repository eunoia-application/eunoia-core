package ru.eunoia.application.port.in;

import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.model.AuthTokens;

public interface LoginUseCase {

    AuthTokens login(LoginCommand command);

}
