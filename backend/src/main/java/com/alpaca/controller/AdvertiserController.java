package com.alpaca.controller;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.mapper.AdvertiserMapper;
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

@RestController
@RequestMapping("api/advertiser")
@RequiredArgsConstructor
public class AdvertiserController {

  private final IAdvertiserService service;
  private final AdvertiserMapper mapper;

  @GetMapping("/{id}")
  public ResponseEntity<AdvertiserResponseDTO> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
  }

  @PostMapping("/save")
  public ResponseEntity<AdvertiserResponseDTO> save(
      @Valid @RequestBody AdvertiserRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AdvertiserResponseDTO> updateById(
      @Valid @RequestBody AdvertiserRequestDTO request, @PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/all")
  public ResponseEntity<List<AdvertiserResponseDTO>> getAll() {
    return ResponseEntity.status(HttpStatus.OK).body(mapper.toListResponseDTO(service.findAll()));
  }

  @GetMapping("/all-page")
  public ResponseEntity<PagedModel<AdvertiserResponseDTO>> findAllPage(Pageable pageable) {
    ;
    return ResponseEntity.status(HttpStatus.OK)
        .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
  }
}
