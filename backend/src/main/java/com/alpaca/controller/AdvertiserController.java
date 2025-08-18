package com.alpaca.controller;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.mapper.IAdvertiserMapper;
import com.alpaca.service.IAdvertiserService;
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
 * REST controller for managing {@link Advertiser} entities.
 *
 * <p>Provides endpoints for CRUD operations and pagination of advertisers. Utilizes {@link
 * IAdvertiserService} for business logic and {@link IAdvertiserMapper} for DTO conversions.
 *
 * @see IAdvertiserService
 * @see IAdvertiserMapper
 */
@RestController
@RequestMapping("/api/advertiser")
@RequiredArgsConstructor
public class AdvertiserController {

    private final IAdvertiserService service;
    private final IAdvertiserMapper mapper;

    /**
     * Retrieves an advertiser by its unique identifier.
     *
     * @param id the unique identifier of the advertiser; must not be {@code null}
     * @return {@link ResponseEntity} containing the {@link AdvertiserResponseDTO} with status
     *     {@link HttpStatus#OK}
     * @throws NotFoundException if no advertiser is found with the given {@code id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdvertiserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    /**
     * Creates a new advertiser.
     *
     * @param request the {@link AdvertiserRequestDTO} containing the advertiser's details; must not
     *     be {@code null}
     * @return {@link ResponseEntity} containing the created {@link AdvertiserResponseDTO} with
     *     status {@link HttpStatus#CREATED}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PostMapping("/save")
    public ResponseEntity<AdvertiserResponseDTO> save(
            @Valid @RequestBody AdvertiserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    /**
     * Updates an existing advertiser identified by its unique identifier.
     *
     * @param request the {@link AdvertiserRequestDTO} containing the updated advertiser's details;
     *     must not be {@code null}
     * @param id the unique identifier of the advertiser to update; must not be {@code null}
     * @return {@link ResponseEntity} containing the updated {@link AdvertiserResponseDTO} with
     *     status {@link HttpStatus#OK}
     * @throws NotFoundException if no advertiser is found with the given {@code id}
     * @throws BadRequestException if the {@code request} is {@code null} or contains invalid data
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdvertiserResponseDTO> updateById(
            @Valid @RequestBody AdvertiserRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    /**
     * Deletes an advertiser identified by its unique identifier.
     *
     * @param id the unique identifier of the advertiser to delete; must not be {@code null}
     * @return {@link ResponseEntity} with status {@link HttpStatus#NO_CONTENT}
     * @throws NotFoundException if no advertiser is found with the given {@code id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all advertisers.
     *
     * @return {@link ResponseEntity} containing a list of {@link AdvertiserResponseDTO} with status
     *     {@link HttpStatus#OK}
     */
    @GetMapping("/all")
    public ResponseEntity<List<AdvertiserResponseDTO>> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    /**
     * Retrieves a paginated list of advertisers.
     *
     * @param pageable the pagination information; must not be {@code null}
     * @return {@link ResponseEntity} containing a {@link PagedModel} of {@link
     *     AdvertiserResponseDTO} with status {@link HttpStatus#OK}
     */
    @GetMapping("/all-page")
    public ResponseEntity<PagedModel<AdvertiserResponseDTO>> findAllPage(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
