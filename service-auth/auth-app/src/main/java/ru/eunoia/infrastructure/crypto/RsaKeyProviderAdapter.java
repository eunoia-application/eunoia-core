package ru.eunoia.infrastructure.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.out.KeyProviderPort;

@Component
public class RsaKeyProviderAdapter implements KeyProviderPort {

    @Override
    public PrivateKey getPrivateKey() {
        return null;
    }

    @Override
    public PublicKey getPublicKey() {
        return null;
    }

    @Override
    public String getKeyId() {
        return "";
    }
}
