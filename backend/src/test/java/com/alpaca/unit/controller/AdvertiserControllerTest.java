package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alpaca.controller.AdvertiserController;
import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import com.alpaca.mapper.IAdvertiserMapper;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.WithMockCustomUser;
import com.alpaca.service.IAdvertiserService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AdvertiserController.class)
@WithMockCustomUser
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class AdvertiserControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JacksonTester<AdvertiserRequestDTO> requestJson;

    @MockitoBean private IAdvertiserService service;

    @MockitoBean private IAdvertiserMapper mapper;

    private final List<Advertiser> listEntities = AdvertiserProvider.listEntities();
    private final Advertiser singleEntity = AdvertiserProvider.singleEntity();
    private final AdvertiserRequestDTO singleRequest = AdvertiserProvider.singleRequest();
    private final AdvertiserResponseDTO firstResponse = AdvertiserProvider.singleResponse();

    @Test
    @DisplayName("findById: Should return 200 OK and advertiser data")
    void findById_ShouldReturnAdvertiser() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(get("/api/advertisers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.title", is(firstResponse.title())));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("findById: Should return 404 Not Found when entity does not exist")
    void findById_ShouldReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/advertisers/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("save: Should return 201 Created when DTO is valid")
    void save_ShouldCreateAdvertiser() throws Exception {
        when(mapper.toEntity(any(AdvertiserRequestDTO.class))).thenReturn(singleEntity);
        when(service.save(singleEntity)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(
                        post("/api/advertisers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())));

        verify(service).save(singleEntity);
    }

    @Test
    @DisplayName("save: Should return 400 Bad Request when validation fails")
    void save_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        AdvertiserRequestDTO invalidRequest = new AdvertiserRequestDTO();
        invalidRequest.setUserId("not-a-uuid");

        mockMvc.perform(
                        post("/api/advertisers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(invalidRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateById: Should return 200 OK after successful update")
    void updateById_ShouldUpdateAdvertiser() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(any(AdvertiserRequestDTO.class))).thenReturn(singleEntity);
        when(service.updateById(singleEntity, id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(
                        put("/api/advertisers/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(firstResponse.title())));

        ArgumentCaptor<Advertiser> captor = ArgumentCaptor.forClass(Advertiser.class);
        verify(service).updateById(captor.capture(), eq(id));
        assertEquals(singleEntity.getTitle(), captor.getValue().getTitle());
    }

    @Test
    @DisplayName("delete: Should return 204 No Content")
    void delete_ShouldReturnNoContent() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/advertisers/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll: Should return list of all advertisers")
    void findAll_ShouldReturnList() throws Exception {
        List<AdvertiserResponseDTO> responseList = AdvertiserProvider.listResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(responseList);

        mockMvc.perform(get("/api/advertisers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(responseList.size())));
    }

    @Test
    @DisplayName("findAllPageForAdmin: Should return paged model for admin")
    void findAllPageForAdmin_ShouldReturnPagedModel() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Advertiser> entityPage = new PageImpl<>(listEntities, pageable, 10);
        Page<AdvertiserResponseDTO> responsePage =
                new PageImpl<>(AdvertiserProvider.listResponse(), pageable, 10);

        when(service.findAllPage(any(Pageable.class))).thenReturn(entityPage);
        when(mapper.toPageResponseDTO(entityPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/advertisers/page-admin").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).findAllPage(any(Pageable.class));
    }

    @Test
    @DisplayName("findAllPage: Should return paged model of indexed advertisers")
    void findAllPage_ShouldReturnIndexedPagedModel() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Advertiser> entityPage = new PageImpl<>(listEntities, pageable, 10);
        Page<AdvertiserResponseDTO> responsePage =
                new PageImpl<>(AdvertiserProvider.listResponse(), pageable, 10);

        when(service.findAllPageByIndexedTrue(any(Pageable.class))).thenReturn(entityPage);
        when(mapper.toPageResponseDTO(entityPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/advertisers/page").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(firstResponse.id().toString())));

        verify(service).findAllPageByIndexedTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("findAll: Should return empty list when no data exists")
    void findAll_ShouldReturnEmpty() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/advertisers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
