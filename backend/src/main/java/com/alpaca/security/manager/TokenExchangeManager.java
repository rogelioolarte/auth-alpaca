package com.alpaca.security.manager;

import com.alpaca.model.AuthCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages single-use authorization code exchange using an in-memory Caffeine cache.
 *
 * <p>When a user completes an OAuth2 login flow ({@link
 * com.alpaca.security.oauth2.AuthSuccessHandler}), an {@link AuthCode} is stored here and the
 * client is redirected to the frontend with the exchange code. The frontend then exchanges this
 * code for actual tokens by calling a token endpoint, which calls {@link #consumeCode(String)}.
 *
 * <p>Key security properties:
 *
 * <ul>
 *   <li><strong>Single-use:</strong> {@link #consumeCode(String)} removes the entry after reading
 *       it, so replay attacks using the same code will fail.
 *   <li><strong>Expiration:</strong> Codes expire after 60 seconds (configured via {@link
 *       Caffeine#expireAfterWrite}), limiting the window for interception.
 *   <li><strong>Memory bound:</strong> Maximum 1000 entries prevents unbounded memory growth under
 *       concurrent login attempts.
 * </ul>
 *
 * <p>This class is thread-safe as {@link Caffeine} provides concurrent access semantics internally.
 *
 * @see AuthCode
 * @see Cache
 * @see Caffeine
 */
@Component
@AllArgsConstructor
public class TokenExchangeManager {

    private final Cache<String, AuthCode> storage =
            Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(1000).build();

    /**
     * Stores an {@link AuthCode} under the given exchange code key so it can be consumed later.
     *
     * <p>Called by {@link com.alpaca.security.oauth2.AuthSuccessHandler} after a successful OAuth2
     * login to register the temporary exchange code before redirecting the client.
     *
     * @param code the exchange code (must be unique, typically a UUID)
     * @param authCode the authorization payload containing user and metadata
     */
    public void createExchangeCode(String code, AuthCode authCode) {
        storage.put(code, authCode);
    }

    /**
     * Retrieves and immediately invalidates the {@link AuthCode} for the given exchange code.
     *
     * <p>This method enforces <strong>single-use</strong> semantics: the code is deleted from the
     * cache after the first successful read. Subsequent calls with the same code return {@link
     * Optional#empty()}.
     *
     * @param code the exchange code to consume
     * @return an {@link Optional} containing the {@link AuthCode} if found and consumed, or empty
     *     if the code is unknown, expired, or already consumed
     */
    public Optional<AuthCode> consumeCode(String code) {
        AuthCode data = storage.getIfPresent(code);
        if (data != null) {
            storage.invalidate(code);
            return Optional.of(data);
        }
        return Optional.empty();
    }
}
