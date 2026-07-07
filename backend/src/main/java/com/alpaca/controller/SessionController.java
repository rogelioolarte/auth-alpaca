package com.alpaca.controller;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.mapper.ISessionMapper;
import com.alpaca.model.UserPrincipal;
import com.alpaca.service.ISessionService;
import com.alpaca.utils.IsAuthenticated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing the authenticated user's sessions at {@code /api/sessions}.
 *
 * <p>Provides endpoints for listing active sessions ({@code GET /page}), revoking a specific
 * session ({@code DELETE /{id}}), or revoking all sessions ({@code DELETE /all}). All endpoints
 * require authentication via {@code @IsAuthenticated} and are scoped to sessions owned by the
 * current user.
 *
 * @see ISessionService
 * @see ISessionMapper
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ISessionService service;
    private final ISessionMapper mapper;

    /**
     * Retrieves a paginated list of sessions belonging to the currently authenticated user.
     *
     * @param user the currently authenticated user; if {@code null} the request is rejected with
     *     401
     * @param pageable the pagination parameters (page, size, sort); must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link SessionResponseDTO}
     *     with status {@link HttpStatus#OK}, or {@link HttpStatus#UNAUTHORIZED} if not
     *     authenticated
     */
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

    /**
     * Revokes a specific session by its ID for the currently authenticated user.
     *
     * <p>Only sessions owned by the authenticated user can be revoked. This operation is idempotent
     * — attempting to revoke an already-revoked or non-existent session succeeds silently.
     *
     * @param user the currently authenticated user; if {@code null} the request is rejected
     * @param id the unique identifier of the session to revoke; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT} on success, or
     *     {@link HttpStatus#UNAUTHORIZED} if not authenticated
     */
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

    /**
     * Revokes all active sessions for the currently authenticated user.
     *
     * <p>This effectively logs the user out from all devices, including the current session.
     *
     * @param user the currently authenticated user; if {@code null} the request is rejected
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT} on success, or
     *     {@link HttpStatus#UNAUTHORIZED} if not authenticated
     */
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
