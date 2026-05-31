package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alpaca.controller.ProfileController;
import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.mapper.IProfileMapper;
import com.alpaca.resources.provider.ProfileProvider;
import com.alpaca.resources.utility.ControllerUnitTest;
import com.alpaca.resources.utility.WithMockCustomUser;
import com.alpaca.service.IProfileService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@ControllerUnitTest
@WebMvcTest(ProfileController.class)
@WithMockCustomUser
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JacksonTester<ProfileRequestDTO> requestJson;
    @MockitoBean private IProfileService service;
    @MockitoBean private IProfileMapper mapper;

    private final List<Profile> listEntities = ProfileProvider.listEntities();
    private final ProfileResponseDTO firstResponse = ProfileProvider.singleResponse();
    private final Profile singleEntity = ProfileProvider.singleEntity();
    private final ProfileRequestDTO singleRequest = ProfileProvider.singleRequest();

    @Test
    @DisplayName("findById: Should return 200 OK and profile data when found")
    void findById_ShouldReturnProfile() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(get("/api/profiles/{id}", id).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.firstName", is(firstResponse.firstName())))
                .andExpect(jsonPath("$.email", is(firstResponse.email())));

        verify(service).findById(id);
    }

    @Test
    @DisplayName("findById: Should return 404 Not Found when service throws exception")
    void findById_ShouldReturnNotFound() throws Exception {
        UUID id = firstResponse.id();
        when(service.findById(id)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/profiles/{id}", id)).andExpect(status().isNotFound());

        verify(service).findById(id);
    }

    @Test
    @DisplayName("save: Should return 201 Created and the mapped response DTO")
    void save_ShouldCreateProfile() throws Exception {
        when(mapper.toEntity(any(ProfileRequestDTO.class))).thenReturn(singleEntity);
        when(service.save(singleEntity)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(
                        post("/api/profiles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())))
                .andExpect(jsonPath("$.firstName", is(firstResponse.firstName())));

        verify(mapper).toEntity(any(ProfileRequestDTO.class));
        verify(service).save(singleEntity);
    }

    @Test
    @DisplayName("updateById: Should return 200 OK after successful update")
    void updateById_ShouldUpdateProfile() throws Exception {
        UUID id = firstResponse.id();
        when(mapper.toEntity(any(ProfileRequestDTO.class))).thenReturn(singleEntity);
        when(service.updateById(singleEntity, id)).thenReturn(singleEntity);
        when(mapper.toResponseDTO(singleEntity)).thenReturn(firstResponse);

        mockMvc.perform(
                        put("/api/profiles/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(singleRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstResponse.id().toString())));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(service).updateById(captor.capture(), eq(id));
        assertEquals(singleEntity.getFirstName(), captor.getValue().getFirstName());
    }

    @Test
    @DisplayName("delete: Should return 204 No Content upon successful deletion")
    void delete_ShouldReturnNoContent() throws Exception {
        UUID id = firstResponse.id();
        doNothing().when(service).deleteById(id);

        mockMvc.perform(delete("/api/profiles/{id}", id)).andExpect(status().isNoContent());

        verify(service).deleteById(id);
    }

    @Test
    @DisplayName("findAll: Should return 200 OK and list of all profiles")
    void findAll_ShouldReturnList() throws Exception {
        List<ProfileResponseDTO> responseList = ProfileProvider.listResponse();
        when(service.findAll()).thenReturn(listEntities);
        when(mapper.toListResponseDTO(listEntities)).thenReturn(responseList);

        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(responseList.size())))
                .andExpect(jsonPath("$[0].id", is(firstResponse.id().toString())));

        verify(service).findAll();
    }

    @Test
    @DisplayName("findAllPage: Should return 200 OK and PagedModel of profiles")
    void findAllPage_ShouldReturnPagedModel() throws Exception {
        Page<Profile> profilePage = new PageImpl<>(listEntities);
        Page<ProfileResponseDTO> responsePage = new PageImpl<>(ProfileProvider.listResponse());

        when(service.findAllPage(any(Pageable.class))).thenReturn(profilePage);
        when(mapper.toPageResponseDTO(profilePage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/profiles/page").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].firstName", is(firstResponse.firstName())));

        verify(service).findAllPage(any(Pageable.class));
    }

    @Test
    @DisplayName("findAll: Should return empty array when no profiles exist")
    void findAll_ShouldReturnEmptyList() throws Exception {
        when(service.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toListResponseDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
