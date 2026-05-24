package com.alpaca.controller;

import com.alpaca.service.ISessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ISessionService sessionService;

    @PostMapping("/{id}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID id) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
