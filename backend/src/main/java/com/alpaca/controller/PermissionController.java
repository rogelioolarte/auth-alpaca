package com.alpaca.controller;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.mapper.IPermissionMapper;
import com.alpaca.service.IPermissionService;
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
 * REST controller for managing {@link Permission} entities.
 *
 * <p>Provides endpoints for CRUD operations and pagination of permissions. Utilizes {@link
 * IPermissionService} for business logic and {@link IPermissionMapper} for DTO conversions.
 *
 * @see IPermissionService
 * @see IPermissionMapper
 */
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionService service;
    private final IPermissionMapper mapper;

    /**
     * Retrieves a {@link PermissionResponseDTO} by its unique identifier.
     *
     * @param id the unique identifier of the permission; must not be {@code null}
     * @return {@link ResponseEntity} containing the {@link PermissionResponseDTO} with status
     *     {@link HttpStatus#OK}
     * @throws NotFoundException if no permission is found with the given {@code id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    /**
     * Creates a new {@link com.alpaca.entity.Permission}.
     *
     * @param request the {@link PermissionRequestDTO} containing the permission's details; must not
     *     be {@code null}
     * @return {@link ResponseEntity} containing the created {@link PermissionResponseDTO} with
     *     status {@link HttpStatus#CREATED}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PostMapping("/save")
    public ResponseEntity<PermissionResponseDTO> save(
            @Valid @RequestBody PermissionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    /**
     * Updates an existing {@link com.alpaca.entity.Permission} identified by its unique identifier.
     *
     * @param request the {@link PermissionRequestDTO} containing the updated permission's details;
     *     must not be {@code null}
     * @param id the unique identifier of the permission to update; must not be {@code null}
     * @return {@link ResponseEntity} containing the updated {@link PermissionResponseDTO} with
     *     status {@link HttpStatus#OK}
     * @throws NotFoundException if no permission is found with the given {@code id}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponseDTO> updateById(
            @Valid @RequestBody PermissionRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    /**
     * Deletes a {@link com.alpaca.entity.Permission} identified by its unique identifier.
     *
     * @param id the unique identifier of the permission to delete; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT}
     * @throws NotFoundException if no permission is found with the given {@code id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all {@link PermissionResponseDTO} entities.
     *
     * @return {@link ResponseEntity} containing a list of {@link PermissionResponseDTO} with status
     *     {@link HttpStatus#OK}
     */
    @GetMapping("/all")
    public ResponseEntity<List<PermissionResponseDTO>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    /**
     * Retrieves a paginated list of {@link PermissionResponseDTO} entities.
     *
     * @param pageable the pagination information; must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link
     *     PermissionResponseDTO} with status {@link HttpStatus#OK}
     */
    @GetMapping("/all-page")
    public ResponseEntity<PagedModel<PermissionResponseDTO>> findAllPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
