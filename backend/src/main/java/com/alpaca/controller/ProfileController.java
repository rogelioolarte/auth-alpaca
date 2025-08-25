package com.alpaca.controller;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.mapper.IProfileMapper;
import com.alpaca.service.IProfileService;
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
 * REST controller for managing {@link Profile} entities.
 *
 * <p>Provides endpoints for CRUD operations and pagination of profiles. Utilizes {@link
 * IProfileService} for business logic and {@link IProfileMapper} for DTO conversions.
 *
 * @see IProfileService
 * @see IProfileMapper
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final IProfileService service;
    private final IProfileMapper mapper;

    /**
     * Retrieves a profile by its unique identifier.
     *
     * @param id the unique identifier of the profile; must not be {@code null}
     * @return {@link ResponseEntity} containing the {@link ProfileResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws NotFoundException if no profile is found with the given {@code id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    /**
     * Creates a new profile.
     *
     * @param request the {@link ProfileRequestDTO} containing the profile's details; must not be
     *     {@code null}
     * @return {@link ResponseEntity} containing the created {@link ProfileResponseDTO} with status
     *     {@link HttpStatus#CREATED}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PostMapping
    public ResponseEntity<ProfileResponseDTO> save(@Valid @RequestBody ProfileRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    /**
     * Updates an existing profile identified by its unique identifier.
     *
     * @param request the {@link ProfileRequestDTO} containing the updated profile's details; must
     *     not be {@code null}
     * @param id the unique identifier of the profile to update; must not be {@code null}
     * @return {@link ResponseEntity} containing the updated {@link ProfileResponseDTO} with status
     *     {@link HttpStatus#OK}
     * @throws NotFoundException if no profile is found with the given {@code id}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponseDTO> updateById(
            @Valid @RequestBody ProfileRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    /**
     * Deletes a profile identified by its unique identifier.
     *
     * @param id the unique identifier of the profile to delete; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT}
     * @throws NotFoundException if no profile is found with the given {@code id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all profiles.
     *
     * @return {@link ResponseEntity} containing a list of {@link ProfileResponseDTO} with status
     *     {@link HttpStatus#OK}
     */
    @GetMapping
    public ResponseEntity<List<ProfileResponseDTO>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    /**
     * Retrieves a paginated list of profiles.
     *
     * @param pageable the pagination information; must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link ProfileResponseDTO}
     *     with status {@link HttpStatus#OK}
     */
    @GetMapping("/page")
    public ResponseEntity<PagedModel<ProfileResponseDTO>> findAllPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
