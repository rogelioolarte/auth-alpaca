package com.alpaca.exception;

/**
 * Thrown when the OAuth2 authentication flow fails — during token exchange, user info retrieval, or
 * provider callback processing.
 *
 * <p>This is a non-HTTP-specific runtime exception (as opposed to the {@link RuntimeException}
 * hierarchy) because the error can occur before an HTTP response context is fully established, such
 * as inside Spring Security's OAuth2 filter chain.
 */
public class OAuth2AuthenticationProcessingException extends RuntimeException {
    /**
     * Constructs a new OAuth2AuthenticationProcessingException with the given detail message.
     *
     * @param message the detail message explaining why OAuth2 processing failed
     */
    public OAuth2AuthenticationProcessingException(String message) {
        super(message);
    }
}
