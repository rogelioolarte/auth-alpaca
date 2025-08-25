package com.alpaca.security.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Spring component responsible for handling secure password hashing and verification using PBKDF2.
 * It wraps a configured {@link Pbkdf2PasswordEncoder} to encode raw passwords and validate matches.
 *
 * <p>The encoder is configured via application property {@code spring.datasource.secret.key}, which
 * serves as the "pepper" — an application-wide secret added on top of per-password salts for
 * enhanced security. ({@link Pbkdf2PasswordEncoder})
 *
 * @see Pbkdf2PasswordEncoder
 */
@Component
public class PasswordManager {

    private final Pbkdf2PasswordEncoder encoder;

    /**
     * Constructs a {@code PasswordManager} using a secret key as "pepper". The encoder is
     * initialized with the following properties:
     *
     * <ul>
     *   <li>Salt length: 16 bytes (default for Spring Security 5.8+)
     *   <li>Iterations: 310,000 (default aiming for ~0.5 seconds processing time)
     *   <li>Algorithm: PBKDF2WithHmacSHA512
     *   <li>Output encoding: Base64 (instead of hex)
     * </ul>
     *
     * @param secretKey the application-wide secret ("pepper") to enhance password hashing security;
     *     must not be {@code null}
     */
    public PasswordManager(@Value("${spring.datasource.secret.key}") @NonNull String secretKey) {
        encoder =
                new Pbkdf2PasswordEncoder(
                        secretKey,
                        16,
                        310000,
                        Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
        encoder.setEncodeHashAsBase64(true);
    }

    /**
     * Exposes the underlying {@link PasswordEncoder}. Useful when integration with Spring Security
     * configurations is needed.
     *
     * @return the configured {@link PasswordEncoder}
     */
    public PasswordEncoder passwordEncoder() {
        return encoder;
    }

    /**
     * Encodes a raw password using PBKDF2 hashing.
     *
     * @param rawPassword the plain text password
     * @return the hashed password string
     */
    public String encodePassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Validates a raw password against a previously hashed password.
     *
     * @param rawPassword the plain text password to validate
     * @param encodedPassword the stored hashed password
     * @return {@code true} if the raw password matches the encoded one; {@code false} otherwise
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
