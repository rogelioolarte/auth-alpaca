package com.alpaca.security.manager;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.utils.UUIDv7Generator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TokenExchangeManager {

    private final UUIDv7Generator uuidGenerator;

    // Configuration: Expires 60 seconds after being written
    private final Cache<String, AuthResponseDTO> storage =
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(1000) // Security limit to prevent memory attacks
                    .build();

    /** Save the tokens and generate a unique exchange code. */
    public String createExchangeCode(AuthResponseDTO authResponse) {
        String code = uuidGenerator.generate().toString();
        storage.put(code, authResponse);
        return code;
    }

    /** Retrieve the tokens and DELETE the code immediately (Single use). */
    public Optional<AuthResponseDTO> consumeCode(String code) {
        AuthResponseDTO data = storage.getIfPresent(code);
        if (data != null) {
            storage.invalidate(code);
            return Optional.of(data);
        }
        return Optional.empty();
    }
}
