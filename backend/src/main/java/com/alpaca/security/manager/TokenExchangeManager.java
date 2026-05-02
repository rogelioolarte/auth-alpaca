package com.alpaca.security.manager;

import com.alpaca.model.AuthCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TokenExchangeManager {

    // Configuration: Expires 60 seconds after being written
    private final Cache<String, AuthCode> storage =
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(1000) // Security limit to prevent memory attacks
                    .build();

    /** Save the tokens and generate a unique exchange code. */
    public void createExchangeCode(String code, AuthCode authCode) {
        storage.put(code, authCode);
    }

    /** Retrieve the tokens and DELETE the code immediately (Single use). */
    public Optional<AuthCode> consumeCode(String code) {
        AuthCode data = storage.getIfPresent(code);
        if (data != null) {
            storage.invalidate(code);
            return Optional.of(data);
        }
        return Optional.empty();
    }
}
