package ru.eunoia.application.port.in;

import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.model.Authentication;

public interface RegisterUseCase {

    Authentication register(RegisterCommand command);
}
