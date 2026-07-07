package com.alpaca.security.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring component responsible for handling secure password hashing and verification using BCrypt.
 * It wraps a {@link BCryptPasswordEncoder} to encode raw passwords and validate matches.
 *
 * @see BCryptPasswordEncoder
 */
@Component
public class PasswordManager {

    private final BCryptPasswordEncoder encoder;

    /**
     * Constructs a {@code PasswordManager} initialized with a cost factor of 12. This value
     * provides a strong balance between security and performance for low-resource environments.
     */
    public PasswordManager(@Value("${security.password.bcrypt.cost-factor:12}") int costFactor) {
        this.encoder = new BCryptPasswordEncoder(costFactor);
    }

    /**
     * Encodes a raw password using BCrypt hashing.
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
