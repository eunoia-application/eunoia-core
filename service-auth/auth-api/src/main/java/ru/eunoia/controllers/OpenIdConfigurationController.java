package ru.eunoia.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/.well-known")
public class OpenIdConfigurationController {

    @GetMapping("/openid-configuration")
    public Map<String, Object> openIdConfiguration() {

        return Map.of(
                "issuer", "service-auth",
                "jwks_uri", "lb://SERVICE-AUTHORIZATION/.well-known/jwks.json",
                "id_token_signing_alg_values_supported", List.of("RS256")
        );
    }

}
