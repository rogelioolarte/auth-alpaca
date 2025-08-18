package com.alpaca.unit.controller;

import com.alpaca.controller.AdvertiserController;
import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import com.alpaca.mapper.AdvertiserMapper;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.service.IAdvertiserService;
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

@WebMvcTest(AdvertiserController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class AdvertiserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JacksonTester<AdvertiserRequestDTO> requestJson;
    @Autowired private JacksonTester<AdvertiserResponseDTO> responseJson;

    @MockitoBean private IAdvertiserService service;
    @MockitoBean private AdvertiserMapper mapper;

    private static final List<Advertiser> listEntities = AdvertiserProvider.listEntities();
    private static final AdvertiserResponseDTO firstResponse = AdvertiserProvider.singleResponse();
    private static final Advertiser singleEntity = AdvertiserProvider.singleEntity();
    private static final AdvertiserRequestDTO singleRequest = AdvertiserProvider.singleRequest();

    private void mockMapperAndServiceForSave() {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getTitle().equals(singleRequest.getTitle())
                                                && r.getDescription()
                                                        .equals(singleRequest.getDescription())
                                                && r.getPublicLocation()
                                                        .equals(
                                                                singleRequest
                                                                        .getPublicLocation()))))
                .thenReturn(singleEntity);
        when(service.save(singleEntity)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    private void mockMapperAndServiceForUpdate(UUID id) {
        when(mapper.toEntity(
                        argThat(
                                r ->
                                        r != null
                                                && r.getTitle().equals(singleRequest.getTitle())
                                                && r.getDescription()
                                                        .equals(singleRequest.getDescription())
                                                && r.getPublicLocation()
                                                        .equals(
                                                                singleRequest
                                                                        .getPublicLocation()))))
                .thenReturn(singleEntity);
        when(service.updateById(singleEntity, id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);
    }

    @Test
    @DisplayName("findById returns 200 and correct object")
    void findByIdReturnsAdvertiser() throws Exception {
        when(service.findById(firstResponse.id())).thenReturn(listEntities.getFirst());
        when(mapper.toResponseDTO(listEntities.getFirst())).thenReturn(firstResponse);

        mockMvc.perform(
                        get("/api/advertiser/{id}", firstResponse.id())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.title", is(firstResponse.title())))
                .andExpect(jsonPath("$.description", is(firstResponse.description())))
                .andExpect(jsonPath("$.bannerUrl", is(firstResponse.bannerUrl())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.publicLocation", is(firstResponse.publicLocation())))
                .andExpect(jsonPath("$.publicUrlLocation", is(firstResponse.publicUrlLocation())))
                .andExpect(jsonPath("$.indexed", is(firstResponse.indexed())))
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

        mockMvc.perform(get("/api/advertiser/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("save returns 201 when Object is created")
    void saveCreatesAdvertiser() throws Exception {
        mockMapperAndServiceForSave();

        mockMvc.perform(
                        post("/api/advertiser/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.title", is(firstResponse.title())))
                .andExpect(jsonPath("$.description", is(firstResponse.description())))
                .andExpect(jsonPath("$.bannerUrl", is(firstResponse.bannerUrl())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.publicLocation", is(firstResponse.publicLocation())))
                .andExpect(jsonPath("$.publicUrlLocation", is(firstResponse.publicUrlLocation())))
                .andExpect(jsonPath("$.indexed", is(firstResponse.indexed())))
                .andExpect(jsonPath("$.userId", is(firstResponse.userId().toString())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        ArgumentCaptor<Advertiser> captor = ArgumentCaptor.forClass(Advertiser.class);
        verify(mapper).toEntity(isA(AdvertiserRequestDTO.class));
        verify(service).save(isA(Advertiser.class));
        verify(service).save(captor.capture());
        verify(mapper).toResponseDTO(isA(Advertiser.class));

        assertNotNull(captor.getValue());
        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getTitle(), captor.getValue().getTitle());
    }

    @Test
    @DisplayName("save returns 409 Conflict when object already exists")
    void saveConflictWhenAlreadyExists() throws Exception {
        mockMapperAndServiceForSave();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"))
                .when(service)
                .save(isA(Advertiser.class));

        mockMvc.perform(
                        post("/api/advertiser/save")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Already exists")));

        verify(service).save(isA(Advertiser.class));
    }

    @Test
    @DisplayName("updateById returns 200 OK and updated object")
    void updateByIdUpdatesAdvertiser() throws Exception {
        UUID id = firstResponse.id();
        mockMapperAndServiceForUpdate(id);

        mockMvc.perform(
                        put("/api/advertiser/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.title", is(firstResponse.title())))
                .andExpect(jsonPath("$.description", is(firstResponse.description())))
                .andExpect(jsonPath("$.bannerUrl", is(firstResponse.bannerUrl())))
                .andExpect(jsonPath("$.avatarUrl", is(firstResponse.avatarUrl())))
                .andExpect(jsonPath("$.publicLocation", is(firstResponse.publicLocation())))
                .andExpect(jsonPath("$.publicUrlLocation", is(firstResponse.publicUrlLocation())))
                .andExpect(jsonPath("$.indexed", is(firstResponse.indexed())))
                .andExpect(jsonPath("$.userId", is(firstResponse.userId().toString())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        ArgumentCaptor<Advertiser> captor = ArgumentCaptor.forClass(Advertiser.class);
        verify(mapper).toEntity(isA(AdvertiserRequestDTO.class));
        verify(service).updateById(captor.capture(), eq(id));
        verify(mapper).toResponseDTO(isA(Advertiser.class));

        assertEquals(singleEntity.getId(), captor.getValue().getId());
        assertEquals(singleEntity.getTitle(), captor.getValue().getTitle());
    }

    @Test
    @DisplayName("updateById returns 404 Not Found when object to modify does not exist")
    void updateByIdNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(argThat(r -> r.getTitle().equals(singleRequest.getTitle()))))
                .thenReturn(singleEntity);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(service)
                .updateById(singleEntity, id);

        mockMvc.perform(
                        put("/api/advertiser/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not found")));

        verify(service).updateById(singleEntity, id);
    }

    @Test
    @DisplayName("delete returns 204 No Content when object exists")
    void deleteDeletesAdvertiser() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/advertiser/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("delete returns 400 Bad Request when object does not exist")
    void deleteReturnsBadRequestWhenNotExist() throws Exception {
        UUID id = firstResponse.id();

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request: not found"))
                .when(service)
                .deleteById(id);

        mockMvc.perform(delete("/api/advertiser/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: not found")));

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll returns an empty list when no persisted entities")
    void findAllReturnsEmptyList() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/advertiser/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).findAll();
        verify(mapper).toListResponseDTO(Collections.emptyList());
    }

    @Test
    @DisplayName("findAll returns all persisted entities")
    void findAllReturnsPersistedList() throws Exception {
        var altResponse = AdvertiserProvider.alternativeResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(AdvertiserProvider.listResponse());

        mockMvc.perform(get("/api/advertiser/all").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$[0].title", is(firstResponse.title())))
                .andExpect(jsonPath("$[1].id", is(altResponse.id().toString())))
                .andExpect(jsonPath("$[1].title", is(altResponse.title())));

        verify(service).findAll();
        verify(mapper).toListResponseDTO(listEntities);
    }

    @Test
    @DisplayName("findAllPage returns a paged list")
    void findAllPageReturnsPagedList() throws Exception {
        when(service.findAllPage(isA(Pageable.class))).thenReturn(new PageImpl<>(listEntities));
        when(mapper.toPageResponseDTO(argThat(r -> r instanceof PageImpl)))
                .thenReturn(AdvertiserProvider.pageResponse());

        mockMvc.perform(
                        get("/api/advertiser/all-page?page=0&size=10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.content[0].title", is(firstResponse.title())));

        verify(service).findAllPage(isA(Pageable.class));
        verify(mapper).toPageResponseDTO(argThat(r -> r instanceof PageImpl));
    }
}
