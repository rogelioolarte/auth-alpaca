package com.alpaca.unit.security.manager;

import com.alpaca.security.manager.PasswordManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link PasswordManager} */
@DisplayName("PasswordManager Unit Tests")
class PasswordManagerTest {

    private PasswordManager passwordManager;

    private static final String SECRET_KEY = "TestSecretKey";
    private static final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        passwordManager = new PasswordManager(SECRET_KEY);
    }

    @Test
    @DisplayName("encodePassword returns a string different from the raw password")
    void encodePasswordReturnsDifferentString() {
        String encoded = passwordManager.encodePassword(rawPassword);

        assertNotNull(encoded, "The result should not be null");
        assertNotEquals(
                rawPassword,
                encoded,
                "The encoded password should not be equal to the raw password");
    }

    @Test
    @DisplayName("matches should return true when raw matches encoded")
    void matchesReturnsTrueForValidPair() {
        String encoded = passwordManager.encodePassword(rawPassword);

        assertTrue(
                passwordManager.matches(rawPassword, encoded),
                "matches should return true if the password matches");
    }

    @Test
    @DisplayName("matches should return false when raw does not match encoded")
    void matchesReturnsFalseForInvalidPair() {
        String encoded = passwordManager.encodePassword(rawPassword);

        String wrongAttempt = rawPassword + "_wrong";
        assertFalse(
                passwordManager.matches(wrongAttempt, encoded),
                "matches should return false if the password does not match");
    }

    @Test
    @DisplayName("Encoding the same raw twice produces different values (random salt)")
    void encodeProducesDifferentValuesEachTime() {
        String firstEncoded = passwordManager.encodePassword(rawPassword);
        String secondEncoded = passwordManager.encodePassword(rawPassword);

        assertNotEquals(
                firstEncoded,
                secondEncoded,
                "Two calls to encodePassword with the same raw should yield different strings");
    }

    @Test
    @DisplayName("passwordEncoder() returns the same PasswordEncoder instance internally")
    void passwordEncoderReturnsSameInstance() {
        assertSame(
                passwordManager.passwordEncoder(),
                passwordManager.passwordEncoder(),
                "passwordEncoder() should always return the same instance");
    }
}
