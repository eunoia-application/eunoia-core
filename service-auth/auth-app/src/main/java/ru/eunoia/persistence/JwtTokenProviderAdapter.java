package ru.eunoia.persistence;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.out.TokenProviderPort;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * M0: symmetric HMAC signing kept as-is so the module compiles on the new domain.
 * M1 replaces this with RS256 + JWKS (see KeyProviderPort / JwksProvider) and fixes the
 * seconds/ms mixup and the missing token-type check.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProviderPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:2592000}")
    private long refreshTokenExpiration;

    @Override
    public AuthTokens generateTokens(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        LocalDateTime now = LocalDateTime.now();
        return new AuthTokens(
                accessToken,
                refreshToken,
                AuthTokens.BEARER,
                (int) (accessTokenExpiration / 1000),
                user.id(),
                now,
                now.plusSeconds(accessTokenExpiration / 1000),
                now.plusSeconds(refreshTokenExpiration / 1000)
        );
    }

    @Override
    public boolean validateAccessToken(String token) {
        return validateToken(token);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return validateToken(token);
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userIdStr = claims.get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    @Override
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    private String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.id().toString());
        claims.put("username", user.username());
        claims.put("email", user.email());
        claims.put("type", "ACCESS");
        return buildToken(claims, user.email(), accessTokenExpiration);
    }

    private String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.id().toString());
        claims.put("type", "REFRESH");
        return buildToken(claims, user.email(), refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
