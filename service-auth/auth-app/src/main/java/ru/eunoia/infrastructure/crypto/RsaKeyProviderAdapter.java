package ru.eunoia.infrastructure.crypto;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.out.KeyProviderPort;

/**
 * Генерит RSA-2048 ключ для подписи один раз на старте (single-instance dev-издатель).
 * kid = SHA-256 thumbprint ключа, совпадает с `kid` в публикуемом JWKS.
 *
 * Харднинг для деплоя (M5): брать СТАБИЛЬНЫЙ ключ из конфига/keystore, а не генерить —
 * тогда токены переживают рестарт, и все инстансы отдают один JWKS.
 */
@Component
public class RsaKeyProviderAdapter implements KeyProviderPort {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String keyId;

    public RsaKeyProviderAdapter() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            this.keyId = new RSAKey.Builder((RSAPublicKey) publicKey)
                    .build()
                    .computeThumbprint()
                    .toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RSA signing key", e);
        }
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public String getKeyId() {
        return keyId;
    }
}
