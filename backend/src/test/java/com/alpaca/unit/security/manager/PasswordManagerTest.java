package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.security.manager.PasswordManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link PasswordManager}. */
@DisplayName("Unit Tests for PasswordManager")
class PasswordManagerTest {

    private PasswordManager passwordManager;
    private final String rawPassword = "AlpacaSecurePassword2026!";
    private final String blankInput = "   ";

    @BeforeEach
    void setUp() {
        int memoryUsage = 19456;
        int iterationCount = 2;
        passwordManager = new PasswordManager(memoryUsage, iterationCount);
    }

    @Test
    @DisplayName("encodePassword: Should return a non-null hashed string distinct from raw input")
    void encodePassword_ShouldReturnValidHash() {
        String encoded = passwordManager.encodePassword(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$argon2id"), "Hash should follow Argon2id format");
    }

    @Test
    @DisplayName("matches: Should return true when credentials are valid")
    void matches_ShouldReturnTrue_WhenPasswordsMatch() {
        String encoded = passwordManager.encodePassword(rawPassword);

        boolean result = passwordManager.matches(rawPassword, encoded);

        assertTrue(result);
    }

    @Test
    @DisplayName("matches: Should return false when raw password does not match stored hash")
    void matches_ShouldReturnFalse_WhenPasswordsDoNotMatch() {
        String encoded = passwordManager.encodePassword(rawPassword);

        String wrongPassword = "IncorrectPassword123";
        boolean result = passwordManager.matches(wrongPassword, encoded);

        assertFalse(result);
    }

    @Test
    @DisplayName("matches: Should return false when raw password input is null or blank")
    void matches_ShouldReturnFalse_WhenRawPasswordIsInvalid() {
        String encoded = passwordManager.encodePassword(rawPassword);

        assertFalse(passwordManager.matches(null, encoded));
        assertFalse(passwordManager.matches(blankInput, encoded));
    }

    @Test
    @DisplayName("matches: Should return false when encoded password input is null or blank")
    void matches_ShouldReturnFalse_WhenEncodedPasswordIsInvalid() {
        assertFalse(passwordManager.matches(rawPassword, null));
        assertFalse(passwordManager.matches(rawPassword, blankInput));
    }

    @Test
    @DisplayName("passwordEncoder: Should return the configured PasswordEncoder instance")
    void passwordEncoder_ShouldReturnInstance() {
        PasswordEncoder encoder = passwordManager.passwordEncoder();

        assertNotNull(encoder);
        String testHash = encoder.encode(rawPassword);
        assertTrue(encoder.matches(rawPassword, testHash));
    }

    @Test
    @DisplayName(
            "Encoding consistency: Two hashes of the same password should differ but both remain"
                    + " valid")
    void encodePassword_ShouldUseRandomSalt() {
        String firstHash = passwordManager.encodePassword(rawPassword);
        String secondHash = passwordManager.encodePassword(rawPassword);

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordManager.matches(rawPassword, firstHash));
        assertTrue(passwordManager.matches(rawPassword, secondHash));
    }
}
