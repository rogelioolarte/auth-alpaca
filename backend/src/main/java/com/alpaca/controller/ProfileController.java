package com.alpaca.controller;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.mapper.ProfileMapper;
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

@RestController
@RequestMapping("api/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final IProfileService service;
  private final ProfileMapper mapper;

  @GetMapping("/{id}")
  public ResponseEntity<ProfileResponseDTO> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
  }

  @PostMapping("/save")
  public ResponseEntity<ProfileResponseDTO> save(@Valid @RequestBody ProfileRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProfileResponseDTO> updateById(
      @Valid @RequestBody ProfileRequestDTO request, @PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("/all")
  public ResponseEntity<List<ProfileResponseDTO>> getAll() {
    return ResponseEntity.status(HttpStatus.OK).body(mapper.toListResponseDTO(service.findAll()));
  }

  @GetMapping("/all-page")
  public ResponseEntity<PagedModel<ProfileResponseDTO>> findAllPage(Pageable pageable) {
    ;
    return ResponseEntity.status(HttpStatus.OK)
        .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
  }
}
