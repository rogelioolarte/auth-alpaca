package com.alpaca.dto.response;

import java.time.LocalDateTime;

public record ErrorResponseDTO(String apiPath, String message, LocalDateTime errorTime) {}
