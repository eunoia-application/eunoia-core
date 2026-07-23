package ru.eunoia.application.port.out;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface KeyProviderPort {

    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    String getKeyId();

}
