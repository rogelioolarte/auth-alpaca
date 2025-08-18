package com.alpaca.unit.controller;

import com.alpaca.controller.ProfileController;
import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.mapper.ProfileMapper;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.service.IProfileService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JacksonTester<ProfileRequestDTO> requestJson;
    @Autowired private JacksonTester<ProfileResponseDTO> responseJson;

    @MockitoBean private IProfileService service;
    @MockitoBean private ProfileMapper mapper;

    private static final List<Profile> listEntities = ProfileProvider.listEntities();
    private static final ProfileResponseDTO firstResponse = ProfileProvider.singleResponse();
    private static final Profile singleEntity = ProfileProvider.singleEntity();
    private static final ProfileRequestDTO singleRequest = ProfileProvider.singleRequest();

    private void mockMapperAndServiceForSave() {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getFirstName()
                                                        .equals(singleRequest.getFirstName())
                                                && r.getLastName()
                                                        .equals(singleRequest.getLastName())
                                                && r.getAddress()
                                                        .equals(singleRequest.getAddress()))))
                .thenReturn(singleEntity);
        when(service.save(singleEntity)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    private void mockMapperAndServiceForUpdate(UUID id) {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getFirstName()
                                                        .equals(singleRequest.getFirstName())
                                                && r.getLastName()
                                                        .equals(singleRequest.getLastName())
                                                && r.getAddress()
                                                        .equals(singleRequest.getAddress()))))
                .thenReturn(singleEntity);
        when(service.updateById(singleEntity, id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    @Test
    @DisplayName("findById returns 200 and correct object")
    void findByIdReturnsProfile() throws Exception {
        when(service.findById(firstResponse.id())).thenReturn(listEntities.getFirst());
        when(mapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponse);

        mockMvc.perform(
                        get("/api/profile/{id}", firstResponse.id())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.firstName", is(firstResponse.firstName())))
                .andExpect(jsonPath("$.lastName", is(firstResponse.lastName())))
                .andExpect(jsonPath("$.address", is(firstResponse.address())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.userId", is(firstResponse.userId().toString())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        verify(service).findById(firstResponse.id());
        verify(mapper).toResponseDTO(listEntities.getFirst());
    }

    @Test
    @DisplayName("findById returns 404 Not Found when object does not exist")
    void findByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(get("/api/profile/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("save returns 201 when Object is created")
    void saveCreatesProfile() throws Exception {
        mockMapperAndServiceForSave();

        mockMvc.perform(
                        post("/api/profile/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.firstName", is(firstResponse.firstName())))
                .andExpect(jsonPath("$.lastName", is(firstResponse.lastName())))
                .andExpect(jsonPath("$.address", is(firstResponse.address())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.userId", is(firstResponse.userId().toString())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(mapper).toEntity(isA(ProfileRequestDTO.class));
        verify(service).save(isA(Profile.class));
        verify(service).save(captor.capture());
        verify(mapper).toResponseDTO(isA(Profile.class));

        assertNotNull(captor.getValue());
        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getFirstName(), captor.getValue().getFirstName());
    }

    @Test
    @DisplayName("save returns 409 Conflict when object already exists")
    void saveConflictWhenAlreadyExists() throws Exception {
        mockMapperAndServiceForSave();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"))
                .when(service)
                .save(isA(Profile.class));

        mockMvc.perform(
                        post("/api/profile/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Already exists")));

        verify(service).save(isA(Profile.class));
    }

    @Test
    @DisplayName("updateById returns 200 OK and updated object")
    void updateByIdUpdatesProfile() throws Exception {
        UUID id = firstResponse.id();
        mockMapperAndServiceForUpdate(id);

        mockMvc.perform(
                        put("/api/profile/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.firstName", is(firstResponse.firstName())))
                .andExpect(jsonPath("$.lastName", is(firstResponse.lastName())))
                .andExpect(jsonPath("$.address", is(firstResponse.address())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.userId", is(firstResponse.userId().toString())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(mapper).toEntity(isA(ProfileRequestDTO.class));
        verify(service).updateById(captor.capture(), eq(id));
        verify(mapper).toResponseDTO(isA(Profile.class));

        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getFirstName(), captor.getValue().getFirstName());
    }

    @Test
    @DisplayName("updateById returns 404 Not Found when object to modify does not exist")
    void updateByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(argThat(r -> r.getFirstName().equals(singleRequest.getFirstName()))))
                .thenReturn(singleEntity);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(service)
                .updateById(singleEntity, id);

        mockMvc.perform(
                        put("/api/profile/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).updateById(singleEntity, id);
    }

    @Test
    @DisplayName("delete returns 204 No Content when object exists")
    void deleteDeletesProfile() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/profile/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("delete returns 400 Bad Request when object does not exist")
    void deleteReturnsBadRequestWhenNotExist() throws Exception {
        UUID id = firstResponse.id();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request: not found"))
                .when(service)
                .deleteById(id);

        mockMvc.perform(delete("/api/profile/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: not found")));

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll returns an empty list when no persisted entities")
    void findAllReturnsEmptyList() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/profile/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).findAll();
        verify(mapper).toListResponseDTO(Collections.emptyList());
    }

    @Test
    @DisplayName("findAll returns all persisted entities")
    void findAllReturnsPersistedList() throws Exception {
        var altResponse = ProfileProvider.alternativeResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(ProfileProvider.listResponse());

        mockMvc.perform(get("/api/profile/all").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$[0].firstName", is(firstResponse.firstName())))
                .andExpect(jsonPath("$[1].id", is(altResponse.id().toString())))
                .andExpect(jsonPath("$[1].firstName", is(altResponse.firstName())));

        verify(service).findAll();
        verify(mapper).toListResponseDTO(listEntities);
    }

    @Test
    @DisplayName("findAllPage returns a paged list")
    void findAllPageReturnsPagedList() throws Exception {
        when(service.findAllPage(isA(Pageable.class))).thenReturn(new PageImpl<>(listEntities));
        when(mapper.toPageResponseDTO(argThat(r -> r instanceof PageImpl)))
                .thenReturn(ProfileProvider.pageResponse());

        mockMvc.perform(
                        get("/api/profile/all-page?page=0&size=10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.content[0].firstName", is(firstResponse.firstName())));

        verify(service).findAllPage(isA(Pageable.class));
        verify(mapper).toPageResponseDTO(argThat(r -> r instanceof PageImpl));
    }
}
