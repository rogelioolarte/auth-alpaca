package com.alpaca.exception;

/**
 * Thrown when the OAuth2 authentication flow fails — during token exchange, user info retrieval, or
 * provider callback processing.
 *
 * <p>This is a non-HTTP-specific runtime exception (as opposed to the {@link
 * ResponseStatusException} hierarchy) because the error can occur before an HTTP response context
 * is fully established, such as inside Spring Security's OAuth2 filter chain.
 */
public class OAuth2AuthenticationProcessingException extends RuntimeException {
    public OAuth2AuthenticationProcessingException(String message) {
        super(message);
    }
}
