package com.alpaca.security.manager;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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
     * Constructs a {@code PasswordManager} using an application-wide secret ("pepper") loaded from
     * a {@link org.springframework.core.io.Resource}.
     *
     * <p>The underlying {@link Pbkdf2PasswordEncoder} is initialized with the following
     * configuration:
     *
     * <ul>
     *   <li>Salt length: 16 bytes (Spring Security default)
     *   <li>Iterations: 310,000 (targeting ~0.5s processing time on modern hardware)
     *   <li>Algorithm: {@code PBKDF2WithHmacSHA512}
     *   <li>Output encoding: Base64 (instead of hexadecimal)
     * </ul>
     *
     * <p>The resource location is typically configured via the {@code spring.datasource.secret.key}
     * property.
     *
     * @param secretKeyResource a resource pointing to the file containing the application-wide
     *     secret ("pepper"); must not be {@code null} and must contain a non-empty value
     * @throws IllegalStateException if the resource cannot be read or the secret value is empty
     */
    public PasswordManager(
            @Value("${spring.datasource.secret.key}") @NonNull Resource secretKeyResource) {
        String secretKey = loadSecret(secretKeyResource);

        this.encoder =
                new Pbkdf2PasswordEncoder(
                        secretKey,
                        16,
                        310_000,
                        Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
        this.encoder.setEncodeHashAsBase64(true);
    }

    /**
     * Loads and returns the secret value from the given {@link Resource}.
     *
     * <p>The resource is expected to contain a single line with the raw secret value. The content
     * is read using UTF-8, trimmed to remove surrounding whitespace, and validated to ensure it is
     * not empty.
     *
     * @param resource the resource pointing to the secret file
     * @return the secret value as a non-blank {@link String}
     * @throws IllegalStateException if the resource cannot be read or the secret value is empty
     */
    private String loadSecret(Resource resource) {
        try {
            String secret =
                    new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                            .trim();
            if (secret.isEmpty()) {
                throw new IllegalStateException("Secret key file is empty");
            }

            return secret;

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to load password pepper from " + resource.getDescription(), ex);
        }
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
