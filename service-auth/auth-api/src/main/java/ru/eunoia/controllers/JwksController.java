package ru.eunoia.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eunoia.application.port.out.JwksProviderPort;

@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class JwksController {

    private final JwksProviderPort jwksProviderPort;

    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {
        return jwksProviderPort.getJwks();
    }

}
