package ru.eunoia.application.port.out;

import java.util.Map;

public interface JwksProviderPort {

    Map<String, Object> getJwks();

}
