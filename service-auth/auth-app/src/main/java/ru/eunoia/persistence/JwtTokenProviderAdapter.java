package ru.eunoia.persistence;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.KeyProviderPort;
import ru.eunoia.application.port.out.TokenProviderPort;

/**
 * RS256 JWT: подписывает приватным RSA-ключом ({@link KeyProviderPort}) и ставит `kid` в заголовок,
 * чтобы resource-server выбрал нужный ключ из JWKS. TTL в секундах; при валидации сверяем claim `type`.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";

    private final KeyProviderPort keyProvider;

    @Value("${jwt.access-token-expiration:3600}") // seconds (1 hour)
    private long accessTokenTtlSeconds;

    @Value("${jwt.refresh-token-expiration:2592000}") // seconds (30 days)
    private long refreshTokenTtlSeconds;

    @Value("${jwt.issuer:service-auth}")
    private String issuer;

    @Override
    public AuthTokens generateTokens(User user) {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokens(
                buildToken(user, TYPE_ACCESS, accessTokenTtlSeconds),
                buildToken(user, TYPE_REFRESH, refreshTokenTtlSeconds),
                AuthTokens.BEARER,
                (int) accessTokenTtlSeconds,
                user.id(),
                now,
                now.plusSeconds(accessTokenTtlSeconds),
                now.plusSeconds(refreshTokenTtlSeconds)
        );
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return isValidTokenOfType(token, TYPE_REFRESH);
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).get(CLAIM_USER_ID, String.class));
    }

    private String buildToken(User user, String type, long ttlSeconds) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + ttlSeconds * 1000);

        var builder = Jwts.builder()
                .header().keyId(keyProvider.getKeyId()).and()
                .issuer(issuer)
                .subject(user.email())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim(CLAIM_USER_ID, user.id().toString())
                .claim(CLAIM_TYPE, type);

        if (TYPE_ACCESS.equals(type)) {
            builder.claim(CLAIM_USERNAME, user.username());
        }

        return builder.signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256).compact();
    }

    private boolean isValidTokenOfType(String token, String expectedType) {
        try {
            return expectedType.equals(parse(token).get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(keyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
