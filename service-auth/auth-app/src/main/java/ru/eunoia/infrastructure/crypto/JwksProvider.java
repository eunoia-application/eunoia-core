package ru.eunoia.infrastructure.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.out.JwksProviderPort;
import ru.eunoia.application.port.out.KeyProviderPort;

@Component
@RequiredArgsConstructor
public class JwksProvider implements JwksProviderPort {

    private final KeyProviderPort keyProvider;

    @Override
    public Map<String, Object> getJwks() {
        return Map.of(
                "keys", List.of(buildJwk())
        );
    }

    private Map<String, Object> buildJwk() {
        RSAPublicKey publicKey = (RSAPublicKey) keyProvider.getPublicKey();

        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(keyProvider.getKeyId())
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();

        return jwk.toJSONObject();
    }

}
