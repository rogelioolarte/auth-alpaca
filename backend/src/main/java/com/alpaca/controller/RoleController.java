package com.alpaca.controller;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.mapper.RoleMapper;
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

@RestController
@RequestMapping("api/role")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService service;
    private final RoleMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponseDTO(service.findById(id)));
    }

    @PostMapping("/save")
    public ResponseEntity<RoleResponseDTO> save(@Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponseDTO(service.save(mapper.toEntity(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> updateById(
            @Valid @RequestBody RoleRequestDTO request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toResponseDTO(service.updateById(mapper.toEntity(request), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/all")
    public ResponseEntity<List<RoleResponseDTO>> getAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(mapper.toListResponseDTO(service.findAll()));
    }

    @GetMapping("/all-page")
    public ResponseEntity<PagedModel<RoleResponseDTO>> findAllPage(Pageable pageable) {
        ;
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PagedModel<>(mapper.toPageResponseDTO(service.findAllPage(pageable))));
    }
}
