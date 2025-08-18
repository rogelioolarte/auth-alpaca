package com.alpaca.controller;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.mapper.IUserMapper;
import com.alpaca.service.IUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing {@link User} entities.
 *
 * <p>Provides endpoints for CRUD operations and pagination of users. Utilizes {@link IUserService}
 * for business logic and {@link IUserMapper} for DTO conversions.
 *
 * @see IUserService
 * @see IUserMapper
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService service;
    private final IUserMapper mapper;

    /**
     * Retrieves a user by its unique identifier.
     *
     * @param id the unique identifier of the user; must not be {@code null}
     * @return {@link ResponseEntity} containing the {@link UserResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws NotFoundException if no user is found with the given {@code id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    /**
     * Creates a new user.
     *
     * @param request the {@link UserRequestDTO} containing the user's details; must not be {@code
     *     null}
     * @return {@link ResponseEntity} containing the created {@link UserResponseDTO} with status
     *     {@link HttpStatus#CREATED}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PostMapping("/save")
    public ResponseEntity<UserResponseDTO> save(@Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    /**
     * Updates an existing user identified by its unique identifier.
     *
     * @param request the {@link UserRequestDTO} containing the updated user's details; must not be
     *     {@code null}
     * @param id the unique identifier of the user to update; must not be {@code null}
     * @return {@link ResponseEntity} containing the updated {@link UserResponseDTO} with status
     *     {@link HttpStatus#OK}
     * @throws NotFoundException if no user is found with the given {@code id}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateById(
            @Valid @RequestBody UserRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    /**
     * Deletes a user identified by its unique identifier.
     *
     * @param id the unique identifier of the user to delete; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT}
     * @throws NotFoundException if no user is found with the given {@code id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all users.
     *
     * @return {@link ResponseEntity} containing a list of {@link UserResponseDTO} with status
     *     {@link HttpStatus#OK}
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    /**
     * Retrieves a paginated list of users.
     *
     * @param pageable the pagination information; must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link UserResponseDTO}
     *     with status {@link HttpStatus#OK}
     */
    @GetMapping("/all-page")
    public ResponseEntity<PagedModel<UserResponseDTO>> findAllPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
