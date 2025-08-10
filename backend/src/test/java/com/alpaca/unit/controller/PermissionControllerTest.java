package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.alpaca.controller.PermissionController;
import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.mapper.PermissionMapper;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.IPermissionService;
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

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class PermissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JacksonTester<PermissionRequestDTO> requestJson;
    @Autowired private JacksonTester<PermissionResponseDTO> responseJson;

    @MockitoBean private IPermissionService service;
    @MockitoBean private PermissionMapper mapper;

    static final List<Permission> listEntities = PermissionProvider.listEntities();
    static final PermissionResponseDTO firstResponse = PermissionProvider.singleResponse();
    static final Permission singleEntity = PermissionProvider.singleEntity();
    static final PermissionRequestDTO singleRequest = PermissionProvider.singleRequest();

    private void mockMapperAndServiceForSave() {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getPermissionName()
                                                        .equals(
                                                                PermissionControllerTest
                                                                        .singleRequest
                                                                        .getPermissionName()))))
                .thenReturn(PermissionControllerTest.singleEntity);
        when(service.save(PermissionControllerTest.singleEntity))
                .thenReturn(PermissionControllerTest.singleEntity);
        when(mapper.toResponseDTO(PermissionControllerTest.singleEntity))
                .thenReturn(PermissionControllerTest.firstResponse);
    }

    private void mockMapperAndServiceForUpdate(UUID id) {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getPermissionName()
                                                        .equals(
                                                                PermissionControllerTest
                                                                        .singleRequest
                                                                        .getPermissionName()))))
                .thenReturn(PermissionControllerTest.singleEntity);
        when(service.updateById(PermissionControllerTest.singleEntity, id))
                .thenReturn(PermissionControllerTest.singleEntity);
        when(mapper.toResponseDTO(PermissionControllerTest.singleEntity))
                .thenReturn(PermissionControllerTest.firstResponse);
    }

    @Test
    @DisplayName("findById returns 200 and correct object")
    void findByIdReturnsPermission() throws Exception {
        when(service.findById(firstResponse.id())).thenReturn(listEntities.getFirst());
        when(mapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponse);

        mockMvc.perform(
                        get("/api/permission/{id}", firstResponse.id())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.permissionName", is(firstResponse.permissionName())));

        verify(service).findById(firstResponse.id());
        verify(mapper).toResponseDTO(listEntities.getFirst());
    }

    @Test
    @DisplayName("findById returns 404 Not Found when object does not exist")
    void findByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(get("/api/permission/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("save returns 201 when Object is created")
    void saveCreatesPermission() throws Exception {
        mockMapperAndServiceForSave();

        mockMvc.perform(
                        post("/api/permission/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.permissionName", is(firstResponse.permissionName())));

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(mapper).toEntity(isA(PermissionRequestDTO.class));
        verify(service).save(isA(Permission.class));
        verify(service).save(captor.capture());
        verify(mapper).toResponseDTO(isA(Permission.class));

        assertNotNull(captor.getValue());
        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getPermissionName(), captor.getValue().getPermissionName());
    }

    @Test
    @DisplayName("save returns 409 Conflict when object already exists")
    void saveConflictWhenAlreadyExists() throws Exception {
        mockMapperAndServiceForSave();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"))
                .when(service)
                .save(isA(Permission.class));

        mockMvc.perform(
                        post("/api/permission/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Already exists")));

        verify(service).save(isA(Permission.class));
    }

    @Test
    @DisplayName("updateById returns 200 OK and updated object")
    void updateByIdUpdatesPermission() throws Exception {
        UUID id = firstResponse.id();
        mockMapperAndServiceForUpdate(id);

        mockMvc.perform(
                        put("/api/permission/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.permissionName", is(firstResponse.permissionName())));

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(mapper).toEntity(isA(PermissionRequestDTO.class));
        verify(service).updateById(captor.capture(), eq(id));
        verify(mapper).toResponseDTO(isA(Permission.class));

        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getPermissionName(), captor.getValue().getPermissionName());
    }

    @Test
    @DisplayName("updateById returns 404 Not Found when object to modify does not exist")
    void updateByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r.getPermissionName()
                                                .equals(singleRequest.getPermissionName()))))
                .thenReturn(singleEntity);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(service)
                .updateById(singleEntity, id);

        mockMvc.perform(
                        put("/api/permission/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).updateById(singleEntity, id);
    }

    @Test
    @DisplayName("delete returns 204 No Content when object exists")
    void deleteDeletesPermission() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/permission/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("delete returns 400 Bad Request when object does not exist")
    void deleteReturnsBadRequestWhenNotExist() throws Exception {
        UUID id = firstResponse.id();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request: not found"))
                .when(service)
                .deleteById(id);

        mockMvc.perform(delete("/api/permission/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: not found")));

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll returns an empty list when no persisted entities")
    void findAllReturnsEmptyList() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/permission/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).findAll();
        verify(mapper).toListResponseDTO(Collections.emptyList());
    }

    @Test
    @DisplayName("findAll returns all persisted entities")
    void findAllReturnsPersistedList() throws Exception {
        PermissionResponseDTO altResponse = PermissionProvider.alternativeResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(PermissionProvider.listResponse());

        mockMvc.perform(get("/api/permission/all").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$[0].permissionName", is(firstResponse.permissionName())))
                .andExpect(jsonPath("$[1].id", is(altResponse.id().toString())))
                .andExpect(jsonPath("$[1].permissionName", is(altResponse.permissionName())));

        verify(service).findAll();
        verify(mapper).toListResponseDTO(listEntities);
    }

    @Test
    @DisplayName("findAllPage returns a paged list")
    void findAllPageReturnsPagedList() throws Exception {
        when(service.findAllPage(isA(Pageable.class))).thenReturn(new PageImpl<>(listEntities));
        when(mapper.toPageResponseDTO(PermissionProvider.pageEntities()))
                .thenReturn(PermissionProvider.pageResponse());

        mockMvc.perform(
                        get("/api/permission/all-page?page=0&size=10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(firstResponse.id().toString())))
                .andExpect(
                        jsonPath(
                                "$.content[0].permissionName", is(firstResponse.permissionName())));

        verify(service).findAllPage(isA(Pageable.class));
        verify(mapper).toPageResponseDTO(PermissionProvider.pageEntities());
    }
}
