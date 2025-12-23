package com.alpaca.dto.request;

public record AuthLoginRequestDTO(
        String email, String password, String clientId, String userAgent, String clientIp) {}
