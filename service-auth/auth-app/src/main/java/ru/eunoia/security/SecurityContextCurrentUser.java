package ru.eunoia.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Достаёт userId из JWT-принципала, который положил resource-server. */
@Component
public class SecurityContextCurrentUser implements CurrentUser {

    @Override
    public UUID id() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }
}
