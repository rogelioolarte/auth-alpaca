package com.alpaca.unit.controller;

import com.alpaca.controller.PermissionController;
import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.mapper.PermissionMapper;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.IPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  IPermissionService permissionService;
  @MockitoBean
  PermissionMapper permissionMapper;

  static final List<Permission> listEntities = PermissionProvider.listEntities();
  static final PermissionResponseDTO firstResponseDTO = PermissionProvider.singleResponse();
  static final PermissionResponseDTO alternativeResponseDTO = PermissionProvider.alternativeResponse();
  static final List<PermissionResponseDTO> listResponseDTO = PermissionProvider.listResponse();

  @Test
  @DisplayName("findById returns 200 and correct object")
  void findById_returns_permission() throws Exception {
    UUID id = firstResponseDTO.id();
    when(permissionService.findById(id)).thenReturn(listEntities.getFirst());
    when(permissionMapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponseDTO);

    mockMvc.perform(get("/api/permission/{id}", id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(firstResponseDTO.id().toString())))
        .andExpect(jsonPath("$.permissionName", is(firstResponseDTO.permissionName())));
  }

  @Test
  @DisplayName("save returns 201 Created and JSON body")
  void save_creates_permission() throws Exception {
    PermissionRequestDTO req = PermissionProvider.singleRequest();
    Permission permission = PermissionProvider.singleEntity();

    when(permissionMapper.toEntity(req)).thenReturn(permission);
    when(permissionService.save(permission)).thenReturn(permission);
    when(permissionMapper.toResponseDTO(permission)).thenReturn(firstResponseDTO);

    mockMvc.perform(post("/api/permission/save")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(firstResponseDTO.id().toString())))
        .andExpect(jsonPath("$.permissionName", is(firstResponseDTO.permissionName())));
  }

  @Test
  @DisplayName("updateById returns 200 OK and updated object")
  void updateById_updates_permission() throws Exception {
    UUID id = firstResponseDTO.id();
    PermissionRequestDTO req = PermissionProvider.singleRequest();
    when(permissionMapper.toEntity(req)).thenReturn(listEntities.getFirst());
    when(permissionService.updateById(listEntities.getFirst(), id)).thenReturn(listEntities.getFirst());
    when(permissionMapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponseDTO);

    mockMvc.perform(put("/api/permission/{id}", id)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(firstResponseDTO.id().toString())))
        .andExpect(jsonPath("$.permissionName", is(firstResponseDTO.permissionName())));
  }

  @Test
  @DisplayName("delete returns 204 No Content")
  void delete_deletes_permission() throws Exception {
    UUID id = firstResponseDTO.id();
    doNothing().when(permissionService).deleteById(id);

    mockMvc.perform(delete("/api/permission/{id}", id)
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("findAll returns an empty list when no persisted entities")
  void findAll_returns_empty_list() throws Exception {
    when(permissionService.findAll()).thenReturn(Collections.emptyList());
    when(permissionMapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/permission/all"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @DisplayName("findAll returns all persisted entities")
  void findAll_returns_persisted_list() throws Exception {
    when(permissionService.findAll()).thenReturn(listEntities);
    when(permissionMapper.toListResponseDTO(listEntities)).thenReturn(listResponseDTO);

    mockMvc.perform(get("/api/permission/all")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isNotEmpty())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id", is(firstResponseDTO.id().toString())))
        .andExpect(jsonPath("$[0].permissionName", is(firstResponseDTO.permissionName())))
        .andExpect(jsonPath("$[1].id", is(alternativeResponseDTO.id().toString())))
        .andExpect(jsonPath("$[1].permissionName", is(alternativeResponseDTO.permissionName())));
  }

  @Test
  @DisplayName("findAllPage returns a paged list")
  void findAllPage_returns_paged_list() throws Exception {
    when(permissionService.findAllPage(any()))
        .thenReturn(new PageImpl<>(listEntities));
    when(permissionMapper.toPageResponseDTO(PermissionProvider.pageEntities()))
        .thenReturn(PermissionProvider.pageResponse());

    mockMvc.perform(get("/api/permission/all-page?page=0&size=10")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id",
            is(firstResponseDTO.id().toString())))
        .andExpect(jsonPath("$.content[0].permissionName",
            is(firstResponseDTO.permissionName())));
  }
}