package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.alpaca.controller.AuthController;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.UserPrincipalProvider;
import com.alpaca.service.IAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JacksonTester<AuthRequestDTO> requestJson;
    @Autowired private JacksonTester<AuthResponseDTO> responseJson;

    @MockitoBean private IAuthService authService;

    private static final AuthRequestDTO validRequest =
            new AuthRequestDTO("admin@admin.com", "12345678");
    private static final AuthRequestDTO invalidPasswordRequest =
            new AuthRequestDTO("admin@admin.com", "short");

    @Test
    @DisplayName("login returns 200 and token when credentials are valid")
    void loginReturnsToken() throws Exception {
        var token = new AuthResponseDTO("jwt-token-123");
        when(authService.login(eq(validRequest.getEmail()), eq(validRequest.getPassword())))
                .thenReturn(token);

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(validRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("jwt-token-123")));

        verify(authService).login(eq(validRequest.getEmail()), eq(validRequest.getPassword()));
    }

    @Test
    @DisplayName("login returns 401 when credentials are invalid")
    void loginUnauthorizedWhenInvalidCredentials() throws Exception {
        when(authService.login(eq(validRequest.getEmail()), eq(validRequest.getPassword())))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(validRequest).getJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid credentials")));

        verify(authService).login(eq(validRequest.getEmail()), eq(validRequest.getPassword()));
        verify(authService, never()).register(any(), any());
    }

    @Test
    @DisplayName("register returns 200 and token when registration succeeds")
    void registerReturnsToken() throws Exception {
        var token = new AuthResponseDTO("register-token-xyz");
        when(authService.register(eq(validRequest.getEmail()), eq(validRequest.getPassword())))
                .thenReturn(token);

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(validRequest).getJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("register-token-xyz")));

        verify(authService).register(eq(validRequest.getEmail()), eq(validRequest.getPassword()));
    }

    @Test
    @DisplayName("register returns 409 Conflict when user already exists")
    void registerConflictWhenAlreadyExists() throws Exception {
        when(authService.register(eq(validRequest.getEmail()), eq(validRequest.getPassword())))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User already exists"));

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(validRequest).getJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("User already exists")));

        verify(authService).register(eq(validRequest.getEmail()), eq(validRequest.getPassword()));
    }

    @Test
    @DisplayName("register returns 400 when password does not meet validation rules")
    void registerBadRequestWhenInvalidPassword() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(invalidPasswordRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getCurrentUser returns 200 and the authenticated principal")
    void getCurrentUserReturnsPrincipal() throws Exception {
        UserPrincipal principal = UserPrincipalProvider.firstResponse();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        mockMvc.perform(get("/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(principal.getId().toString())))
                .andExpect(jsonPath("$.username", is(principal.getUsername())));
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser returns 403")
    void getCurrentUserReturnsNullWhenNoPrincipal() throws Exception {
        mockMvc.perform(get("/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("getCurrentUser returns 200 and empty body when principal is not UserPrincipal")
    void getCurrentUserReturnsEmptyWhenPrincipalNotUserPrincipal() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("root returns API Online")
    void rootReturnsApiOnline() throws Exception {
        mockMvc.perform(get("/auth/").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("API Online"));
    }
}
