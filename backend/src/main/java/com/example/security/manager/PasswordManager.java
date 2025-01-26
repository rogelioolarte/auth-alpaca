package com.example.security.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordManager {

    private final Pbkdf2PasswordEncoder encoder;

    public PasswordManager(@Value("${spring.datasource.secret.key}")
                           @NonNull String secretKey) {
        encoder = new Pbkdf2PasswordEncoder(
                secretKey, 16, 310000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
        encoder.setEncodeHashAsBase64(true);
    }

    public PasswordEncoder passwordEncoder() {
        return encoder;
    }

    public String encodePassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
