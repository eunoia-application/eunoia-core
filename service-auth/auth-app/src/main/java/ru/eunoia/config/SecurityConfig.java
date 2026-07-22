package ru.eunoia.config;

import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.eunoia.application.port.out.KeyProviderPort;

/**
 * auth сам себе resource-server: валидирует свои же RS256-токены публичным ключом.
 * Публичны только вход/регистрация/refresh/сброс пароля, JWKS и health; остальное — под токеном.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/auth/register", "/auth/login", "/auth/refresh",
            "/auth/forgot-password", "/auth/reset-password", "/auth/verify-email",
            "/.well-known/**", "/actuator/health", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** Декодер валидирует токены нашим же публичным ключом (тот, что публикуется в JWKS). */
    @Bean
    public JwtDecoder jwtDecoder(KeyProviderPort keyProvider) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyProvider.getPublicKey()).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
