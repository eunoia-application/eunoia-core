package ru.eunoia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.eunoia.application.port.out.JwksProviderPort;

/**
 * Отдаёт JWKS ровно так, как его собрал порт — контроллер не преобразует тело.
 */
@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

    @Mock
    private JwksProviderPort jwksProviderPort;

    @InjectMocks
    private JwksController controller;

    @Test
    void jwks_delegatesToPort_andReturnsItsBody() {
        Map<String, Object> jwks = Map.of("keys", List.of(Map.of("kid", "key-1", "kty", "RSA")));
        when(jwksProviderPort.getJwks()).thenReturn(jwks);

        Map<String, Object> result = controller.jwks();

        assertThat(result).isSameAs(jwks);
        verify(jwksProviderPort).getJwks();
    }
}
