package com.alpaca.controller;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.mapper.ISessionMapper;
import com.alpaca.model.UserPrincipal;
import com.alpaca.service.ISessionService;
import com.alpaca.utils.IsAuthenticated;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ISessionService service;
    private final ISessionMapper mapper;

    @IsAuthenticated
    @GetMapping("/page")
    public ResponseEntity<PagedModel<SessionResponseDTO>> findAllPageByUserId(
            @AuthenticationPrincipal UserPrincipal user, Pageable pageable) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        new PagedModel<>(
                                mapper.toPageResponseDTO(
                                        service.findAllByUserId(user.getUserId(), pageable))));
    }

    @IsAuthenticated
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeSessionByUser(
            @AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        service.revokeSessionByUserIdAndId(user.getUserId(), id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @IsAuthenticated
    @DeleteMapping("/all")
    public ResponseEntity<Void> revokeAllSessionsByUser(
            @AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        service.revokeAllSessionsByUserId(user.getUserId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
