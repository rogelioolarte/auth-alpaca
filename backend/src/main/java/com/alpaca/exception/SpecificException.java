package com.alpaca.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class SpecificException extends ResponseStatusException {

    public SpecificException(Integer status, String reason) {
        super(HttpStatusCode.valueOf(status), reason);
    }
}
