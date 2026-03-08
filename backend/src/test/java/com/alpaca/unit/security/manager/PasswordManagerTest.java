package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.security.manager.PasswordManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/** Unit tests for {@link PasswordManager}. */
@DisplayName("PasswordManager Unit Tests")
class PasswordManagerTest {

    private PasswordManager passwordManager;

    private static final String RAW_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        // Use an in-memory resource with a valid secret to avoid relying on external files.
        Resource valid =
                new ByteArrayResource("my-super-secret-pepper".getBytes(StandardCharsets.UTF_8));
        passwordManager = new PasswordManager(valid);
    }

    @Test
    @DisplayName("encodePassword returns a string different from the raw password")
    void encodePasswordReturnsDifferentString() {
        String encoded = passwordManager.encodePassword(RAW_PASSWORD);

        assertNotNull(encoded, "The result should not be null");
        assertNotEquals(RAW_PASSWORD, encoded, "The encoded password must not equal the raw value");
    }

    @Test
    @DisplayName("matches returns true when raw matches encoded")
    void matchesReturnsTrueForValidPair() {
        String encoded = passwordManager.encodePassword(RAW_PASSWORD);

        assertTrue(
                passwordManager.matches(RAW_PASSWORD, encoded),
                "matches should return true for valid pair");
    }

    @Test
    @DisplayName("matches returns false when raw does not match encoded")
    void matchesReturnsFalseForInvalidPair() {
        String encoded = passwordManager.encodePassword(RAW_PASSWORD);

        String wrongAttempt = RAW_PASSWORD + "_wrong";
        assertFalse(
                passwordManager.matches(wrongAttempt, encoded),
                "matches should return false for invalid pair");
    }

    @Test
    @DisplayName("Encoding the same raw twice produces different values (random salt)")
    void encodeProducesDifferentValuesEachTime() {
        String firstEncoded = passwordManager.encodePassword(RAW_PASSWORD);
        String secondEncoded = passwordManager.encodePassword(RAW_PASSWORD);

        assertNotEquals(
                firstEncoded, secondEncoded, "Two encodes of same raw must differ due to salt");
    }

    @Test
    @DisplayName("passwordEncoder() returns the same PasswordEncoder instance internally")
    void passwordEncoderReturnsSameInstance() {
        assertSame(
                passwordManager.passwordEncoder(),
                passwordManager.passwordEncoder(),
                "passwordEncoder() should return the same instance on repeated calls");
    }

    @Test
    @DisplayName("matches returns false when encodedPassword is null")
    void matchesReturnsFalseWhenEncodedIsNull() {
        assertFalse(
                passwordManager.matches(RAW_PASSWORD, null),
                "matches should return false when encoded is null");
    }

    // ---------- error / constructor branches ----------

    @Test
    @DisplayName("constructor throws IllegalStateException when secret file is empty")
    void constructorThrowsWhenSecretEmpty() {
        Resource empty =
                new ByteArrayResource("   ".getBytes(StandardCharsets.UTF_8)); // trims to empty
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> new PasswordManager(empty));
        // Message exception when the file not exists
        assertTrue(ex.getMessage().contains("Unable to load password pepper from"));
    }

    @Test
    @DisplayName("constructor throws IllegalStateException when resource read fails")
    void constructorThrowsWhenResourceReadFails() {
        // Create a Resource that throws IOException on getInputStream
        Resource broken =
                new AbstractResource() {

                    @Override
                    public String getDescription() {
                        return "broken-resource";
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        throw new IOException("simulated IO failure");
                    }
                };

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> new PasswordManager(broken));
        assertTrue(
                ex.getMessage().contains("Unable to load password pepper from"),
                "message should mention inability to load");
        assertTrue(
                ex.getMessage().contains("broken-resource"),
                "message should include resource description");
    }
}
