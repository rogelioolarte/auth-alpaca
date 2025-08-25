package com.alpaca.controller;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.mapper.IRoleMapper;
import com.alpaca.service.IRoleService;
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
 * REST controller for managing {@link Role} entities.
 *
 * <p>Provides endpoints for CRUD operations and pagination of roles. Utilizes {@link IRoleService}
 * for business logic and {@link IRoleMapper} for DTO conversions.
 *
 * @see IRoleService
 * @see IRoleMapper
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService service;
    private final IRoleMapper mapper;

    /**
     * Retrieves a {@link RoleResponseDTO} by its unique identifier.
     *
     * @param id the unique identifier of the role; must not be {@code null}
     * @return {@link ResponseEntity} containing the {@link RoleResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws NotFoundException if no role is found with the given {@code id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    /**
     * Creates a new {@link Role}.
     *
     * @param request the {@link RoleRequestDTO} containing the role's details; must not be {@code
     *     null}
     * @return {@link ResponseEntity} containing the created {@link RoleResponseDTO} with status
     *     {@link HttpStatus#CREATED}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PostMapping
    public ResponseEntity<RoleResponseDTO> save(@Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    /**
     * Updates an existing {@link Role} identified by its unique identifier.
     *
     * @param request the {@link RoleRequestDTO} containing the updated role's details; must not be
     *     {@code null}
     * @param id the unique identifier of the role to update; must not be {@code null}
     * @return {@link ResponseEntity} containing the updated {@link RoleResponseDTO} with status
     *     {@link HttpStatus#OK}
     * @throws NotFoundException if no role is found with the given {@code id}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> updateById(
            @Valid @RequestBody RoleRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    /**
     * Deletes a {@link Role} identified by its unique identifier.
     *
     * @param id the unique identifier of the role to delete; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT}
     * @throws NotFoundException if no role is found with the given {@code id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all {@link RoleResponseDTO}s.
     *
     * @return {@link ResponseEntity} containing a list of {@link RoleResponseDTO}s with status
     *     {@link HttpStatus#OK}
     */
    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    /**
     * Retrieves a paginated list of {@link RoleResponseDTO}s.
     *
     * @param pageable the pagination information; must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link RoleResponseDTO}s
     *     with status {@link HttpStatus#OK}
     */
    @GetMapping("/page")
    public ResponseEntity<PagedModel<RoleResponseDTO>> findAllPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
