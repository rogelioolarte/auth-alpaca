package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.alpaca.controller.RoleController;
import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;
import com.alpaca.mapper.IRoleMapper;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.IRoleService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JacksonTester<RoleRequestDTO> requestJson;
    @Autowired private JacksonTester<RoleResponseDTO> responseJson;

    @MockitoBean private IRoleService service;
    @MockitoBean private IRoleMapper mapper;

    private static final List<Role> listEntities = RoleProvider.listEntities();
    private static final RoleResponseDTO firstResponse = RoleProvider.singleResponse();
    private static final Role singleEntity = RoleProvider.singleEntity();
    private static final RoleRequestDTO singleRequest = RoleProvider.singleRequest();

    private void mockMapperAndServiceForSave() {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getRoleName()
                                                        .equals(singleRequest.getRoleName())
                                                && r.getRoleDescription()
                                                        .equals(
                                                                singleRequest
                                                                        .getRoleDescription()))))
                .thenReturn(singleEntity);
        when(service.save(singleEntity)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    private void mockMapperAndServiceForUpdate(UUID id) {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getRoleName()
                                                        .equals(singleRequest.getRoleName())
                                                && r.getRoleDescription()
                                                        .equals(
                                                                singleRequest
                                                                        .getRoleDescription()))))
                .thenReturn(singleEntity);
        when(service.updateById(singleEntity, id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    @Test
    @DisplayName("findById returns 200 and correct object")
    void findByIdReturnsRole() throws Exception {
        when(service.findById(firstResponse.id())).thenReturn(listEntities.getFirst());
        when(mapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponse);

        mockMvc.perform(
                        get("/api/roles/{id}", firstResponse.id())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.roleName", is(firstResponse.roleName())))
                .andExpect(jsonPath("$.roleDescription", is(firstResponse.roleDescription())));

        verify(service).findById(firstResponse.id());
        verify(mapper).toResponseDTO(listEntities.getFirst());
    }

    @Test
    @DisplayName("findById returns 404 Not Found when object does not exist")
    void findByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(get("/api/roles/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("save returns 201 when Object is created")
    void saveCreatesRole() throws Exception {
        mockMapperAndServiceForSave();

        mockMvc.perform(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.roleName", is(firstResponse.roleName())))
                .andExpect(jsonPath("$.roleDescription", is(firstResponse.roleDescription())));

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(mapper).toEntity(isA(RoleRequestDTO.class));
        verify(service).save(isA(Role.class));
        verify(service).save(captor.capture());
        verify(mapper).toResponseDTO(isA(Role.class));

        assertNotNull(captor.getValue());
        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getRoleName(), captor.getValue().getRoleName());
    }

    @Test
    @DisplayName("save returns 409 Conflict when object already exists")
    void saveConflictWhenAlreadyExists() throws Exception {
        mockMapperAndServiceForSave();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"))
                .when(service)
                .save(isA(Role.class));

        mockMvc.perform(
                        post("/api/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Already exists")));

        verify(service).save(isA(Role.class));
    }

    @Test
    @DisplayName("updateById returns 200 OK and updated object")
    void updateByIdUpdatesRole() throws Exception {
        UUID id = firstResponse.id();
        mockMapperAndServiceForUpdate(id);

        mockMvc.perform(
                        put("/api/roles/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.roleName", is(firstResponse.roleName())))
                .andExpect(jsonPath("$.roleDescription", is(firstResponse.roleDescription())));

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(mapper).toEntity(isA(RoleRequestDTO.class));
        verify(service).updateById(captor.capture(), eq(id));
        verify(mapper).toResponseDTO(isA(Role.class));

        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getRoleName(), captor.getValue().getRoleName());
    }

    @Test
    @DisplayName("updateById returns 404 Not Found when object to modify does not exist")
    void updateByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(argThat(r -> r.getRoleName().equals(singleRequest.getRoleName()))))
                .thenReturn(singleEntity);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(service)
                .updateById(singleEntity, id);

        mockMvc.perform(
                        put("/api/roles/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).updateById(singleEntity, id);
    }

    @Test
    @DisplayName("delete returns 204 No Content when object exists")
    void deleteDeletesRole() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/roles/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("delete returns 400 Bad Request when object does not exist")
    void deleteReturnsBadRequestWhenNotExist() throws Exception {
        UUID id = firstResponse.id();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request: not found"))
                .when(service)
                .deleteById(id);

        mockMvc.perform(delete("/api/roles/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: not found")));

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll returns an empty list when no persisted entities")
    void findAllReturnsEmptyList() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).findAll();
        verify(mapper).toListResponseDTO(Collections.emptyList());
    }

    @Test
    @DisplayName("findAll returns all persisted entities")
    void findAllReturnsPersistedList() throws Exception {
        RoleResponseDTO altResponse = RoleProvider.alternativeResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(RoleProvider.listResponse());

        mockMvc.perform(get("/api/roles").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$[0].roleName", is(firstResponse.roleName())))
                .andExpect(jsonPath("$[1].id", is(altResponse.id().toString())))
                .andExpect(jsonPath("$[1].roleName", is(altResponse.roleName())));

        verify(service).findAll();
        verify(mapper).toListResponseDTO(listEntities);
    }

    @Test
    @DisplayName("findAllPage returns a paged list")
    void findAllPageReturnsPagedList() throws Exception {
        when(service.findAllPage(isA(Pageable.class))).thenReturn(new PageImpl<>(listEntities));
        when(mapper.toPageResponseDTO(argThat(r -> r instanceof PageImpl)))
                .thenReturn(RoleProvider.pageResponse());

        mockMvc.perform(get("/api/roles/page?page=0&size=10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.content[0].roleName", is(firstResponse.roleName())));

        verify(service).findAllPage(isA(Pageable.class));
        verify(mapper).toPageResponseDTO(argThat(r -> r instanceof PageImpl));
    }
}
