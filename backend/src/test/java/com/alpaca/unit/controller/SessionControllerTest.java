package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alpaca.controller.SessionController;
import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.entity.Session;
import com.alpaca.mapper.ISessionMapper;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.utility.WithMockCustomUser;
import com.alpaca.service.ISessionService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ISessionService service;
    @MockitoBean private ISessionMapper mapper;
    @MockitoBean private UserPrincipal userPrincipal;

    private static final SessionResponseDTO response = SessionProvider.singleResponse();
    private static final Session session = SessionProvider.singleEntity();

    @Test
    @DisplayName("findAllPageByUserId returns 200 OK with paged sessions")
    @WithMockCustomUser
    void findAllPageByUserIdReturnsPagedSessions() throws Exception {

        PageImpl<Session> page = new PageImpl<>(List.of(session));

        when(userPrincipal.getUserId()).thenReturn(session.getUser().getId());
        when(service.findAllByUserId(eq(session.getUser().getId()), isA(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponseDTO(argThat(PageImpl.class::isInstance)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(
                        get("/api/sessions/page?page=0&size=10")
                                .principal(() -> userPrincipal.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(response.id().toString())));

        verify(service).findAllByUserId(eq(session.getUser().getId()), isA(Pageable.class));
        verify(mapper).toPageResponseDTO(argThat(PageImpl.class::isInstance));
    }

    @Test
    @DisplayName("findAllPageByUserId returns 401 Unauthorized when user is null")
    void findAllPageByUserIdReturnsUnauthorizedWhenUserIsNull() throws Exception {

        mockMvc.perform(get("/api/sessions/page?page=0&size=10"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("revokeSessionByUser returns 204 No Content")
    @WithMockCustomUser
    void revokeSessionByUserReturnsNoContent() throws Exception {

        UUID sessionId = response.id();

        when(userPrincipal.getUserId()).thenReturn(session.getUser().getId());

        doNothing().when(service).revokeSessionByUserIdAndId(session.getUser().getId(), sessionId);

        mockMvc.perform(
                        delete("/api/sessions/{id}", sessionId)
                                .principal(() -> userPrincipal.getUserId().toString()))
                .andExpect(status().isNoContent());

        verify(service).revokeSessionByUserIdAndId(session.getUser().getId(), sessionId);
    }

    @Test
    @DisplayName("revokeSessionByUser returns 401 Unauthorized when user is null")
    void revokeSessionByUserReturnsUnauthorizedWhenUserIsNull() throws Exception {

        mockMvc.perform(delete("/api/sessions/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("revokeAllSessionsByUser returns 204 No Content")
    @WithMockCustomUser
    void revokeAllSessionsByUserReturnsNoContent() throws Exception {

        when(userPrincipal.getUserId()).thenReturn(session.getUser().getId());

        doNothing().when(service).revokeAllSessionsByUserId(session.getUser().getId());

        mockMvc.perform(
                        delete("/api/sessions/all")
                                .principal(() -> userPrincipal.getUserId().toString()))
                .andExpect(status().isNoContent());

        verify(service).revokeAllSessionsByUserId(session.getUser().getId());
    }

    @Test
    @DisplayName("revokeAllSessionsByUser returns 401 Unauthorized when user is null")
    void revokeAllSessionsByUserReturnsUnauthorizedWhenUserIsNull() throws Exception {

        mockMvc.perform(delete("/api/sessions/all")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("findAllPageByUserId returns empty page")
    @WithMockCustomUser
    void findAllPageByUserIdReturnsEmptyPage() throws Exception {

        PageImpl<Session> page = new PageImpl<>(Collections.emptyList());

        when(userPrincipal.getUserId()).thenReturn(session.getUser().getId());
        when(service.findAllByUserId(eq(session.getUser().getId()), isA(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponseDTO(argThat(PageImpl.class::isInstance)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(
                        get("/api/sessions/page?page=0&size=10")
                                .principal(() -> userPrincipal.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).findAllByUserId(eq(session.getUser().getId()), isA(Pageable.class));
        verify(mapper).toPageResponseDTO(argThat(PageImpl.class::isInstance));
    }
}
