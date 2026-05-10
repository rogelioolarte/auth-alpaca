package com.alpaca.security.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring component responsible for handling secure password hashing and verification using
 * Argon2Id. It wraps a configured {@link Argon2PasswordEncoder} to encode raw passwords and
 * validate matches.
 *
 * @see Argon2PasswordEncoder
 */
@Component
public class PasswordManager {

    private final Argon2PasswordEncoder encoder;

    /**
     * Constructs a {@code PasswordManager}
     *
     * <p>The underlying {@link Argon2PasswordEncoder} is initialized with the following
     * configuration:
     *
     * <ul>
     *   <li>Salt length: 16 bytes (Spring Security default)
     *   <li>Iterations: 2
     *   <li>Algorithm: {@code Argon2PasswordEncoder}
     * </ul>
     */
    public PasswordManager(
            @Value("${security.argon2.memory:19456}") int memory,
            @Value("${security.argon2.iterations:2}") int iterations) {
        this.encoder = new Argon2PasswordEncoder(16, 32, 1, memory, iterations);
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
     * Encodes a raw password using Argon2Id hashing.
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
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
