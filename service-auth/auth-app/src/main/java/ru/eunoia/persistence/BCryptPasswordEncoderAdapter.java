package ru.eunoia.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.eunoia.application.port.out.PasswordEncoderPort;

@Component
@RequiredArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

}
