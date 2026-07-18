package ru.eunoia.application.port.in;

import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.AuthTokens;

public interface RegisterUseCase {

    AuthTokens register(RegisterCommand command);

}
