package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.model.AuthCode;
import com.alpaca.security.manager.TokenExchangeManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Tests for TokenExchangeManager")
class TokenExchangeManagerTest {

    private TokenExchangeManager tokenExchangeManager;
    private final AuthCode authCode =
            new AuthCode(
                    "test-code",
                    "test-verifier",
                    "http://localhost/callback",
                    "client-id",
                    "Mozilla/5.0",
                    "127.0.0.1");
    private final String exchangeKey = "unique-exchange-key-123";

    @BeforeEach
    void setUp() {
        tokenExchangeManager = new TokenExchangeManager();
    }

    @Test
    @DisplayName("consumeCode: Should return AuthCode and then invalidate it for single use")
    void consumeCode_ShouldReturnDataAndInvalidate() {
        tokenExchangeManager.createExchangeCode(exchangeKey, authCode);

        Optional<AuthCode> firstAttempt = tokenExchangeManager.consumeCode(exchangeKey);
        Optional<AuthCode> secondAttempt = tokenExchangeManager.consumeCode(exchangeKey);

        assertTrue(firstAttempt.isPresent(), "First attempt should return data");
        assertEquals(
                authCode.getCode(),
                firstAttempt.get().getCode(),
                "Returned code should match the saved one");
        assertEquals(
                authCode.getClientId(),
                firstAttempt.get().getClientId(),
                "Returned client ID should match");

        assertTrue(
                secondAttempt.isEmpty(),
                "Second attempt should be empty as code must be invalidated after consumption");
    }

    @Test
    @DisplayName("consumeCode: Should return empty Optional when code does not exist")
    void consumeCode_ShouldReturnEmpty_WhenCodeNotFound() {
        String nonExistentKey = "wrong-key";

        Optional<AuthCode> result = tokenExchangeManager.consumeCode(nonExistentKey);

        assertTrue(result.isEmpty(), "Result should be empty for non-existent keys");
    }

    @Test
    @DisplayName("createExchangeCode: Should allow overwriting or updating existing codes")
    void createExchangeCode_ShouldAllowUpdates() {
        AuthCode newAuthCode =
                new AuthCode(
                        "new-code",
                        "new-verifier",
                        "http://localhost",
                        "new-client",
                        "Agent",
                        "0.0.0.0");

        tokenExchangeManager.createExchangeCode(exchangeKey, authCode);
        tokenExchangeManager.createExchangeCode(exchangeKey, newAuthCode);

        Optional<AuthCode> result = tokenExchangeManager.consumeCode(exchangeKey);

        assertTrue(result.isPresent());
        assertEquals(newAuthCode.getCode(), result.get().getCode());
    }
}
