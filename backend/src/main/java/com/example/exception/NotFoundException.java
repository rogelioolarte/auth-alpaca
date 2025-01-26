package com.example.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
@Setter
public class NotFoundException extends ResponseStatusException {

    public NotFoundException (String message){
        super(HttpStatus.NOT_FOUND, message);
    }
}
