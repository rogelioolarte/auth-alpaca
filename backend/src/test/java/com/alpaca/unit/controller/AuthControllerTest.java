package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alpaca.controller.AuthController;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.WithMockCustomUser;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJsonTesters
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JacksonTester<AuthRequestDTO> requestJson;

    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private IAuthService authService;

    @MockitoBean private AuthenticationManager manager;

    @MockitoBean private Authentication authentication;

    @MockitoBean private UserPrincipal userPrincipal;

    private MockedStatic<Utils> utilsMock;

    private static final String CLIENT_ID = "client-id";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String CLIENT_IP = "192.168.1.10";

    private static final AuthRequestDTO REQUEST =
            new AuthRequestDTO("test@alpaca.com", "password123");

    private static final AuthResponseDTO RESPONSE =
            new AuthResponseDTO("access-token", "refresh-token");

    @AfterEach
    void tearDown() {
        if (utilsMock != null) {
            utilsMock.close();
        }
    }

    private void mockClientIp() {
        utilsMock = mockStatic(Utils.class);
        utilsMock
                .when(() -> Utils.extractClientIP(any(HttpServletRequest.class)))
                .thenReturn(CLIENT_IP);
    }

    @Test
    @DisplayName("login returns 200 OK and authentication response")
    void loginReturnsAuthenticationResponse() throws Exception {
        mockClientIp();

        UsernamePasswordAuthenticationToken expectedToken =
                new UsernamePasswordAuthenticationToken(REQUEST.getEmail(), REQUEST.getPassword());

        when(manager.authenticate(
                        argThat(
                                token ->
                                        token instanceof UsernamePasswordAuthenticationToken
                                                && Objects.equals(
                                                        token.getPrincipal(),
                                                        expectedToken.getPrincipal())
                                                && Objects.equals(
                                                        token.getCredentials(),
                                                        expectedToken.getCredentials()))))
                .thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        when(authService.login(
                        eq(userPrincipal),
                        argThat(
                                dto ->
                                        dto.email().equals(REQUEST.getEmail())
                                                && dto.password().equals(REQUEST.getPassword())
                                                && dto.clientId().equals(CLIENT_ID)
                                                && dto.userAgent().equals(USER_AGENT)
                                                && dto.clientIp().equals(CLIENT_IP))))
                .thenReturn(RESPONSE);

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(REQUEST).getJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(RESPONSE.accessToken())))
                .andExpect(jsonPath("$.refreshToken", is(RESPONSE.refreshToken())));

        verify(manager)
                .authenticate(
                        argThat(
                                token ->
                                        token instanceof UsernamePasswordAuthenticationToken
                                                && Objects.equals(
                                                        token.getPrincipal(), REQUEST.getEmail())
                                                && Objects.equals(
                                                        token.getCredentials(),
                                                        REQUEST.getPassword())));

        verify(authService)
                .login(
                        eq(userPrincipal),
                        argThat(
                                dto ->
                                        dto.email().equals(REQUEST.getEmail())
                                                && dto.password().equals(REQUEST.getPassword())
                                                && dto.clientId().equals(CLIENT_ID)
                                                && dto.userAgent().equals(USER_AGENT)
                                                && dto.clientIp().equals(CLIENT_IP)));
    }

    @Test
    @DisplayName("register returns 200 OK and authentication response")
    void registerReturnsAuthenticationResponse() throws Exception {
        mockClientIp();

        when(authService.register(
                        argThat(
                                dto ->
                                        dto.email().equals(REQUEST.getEmail())
                                                && dto.password().equals(REQUEST.getPassword())
                                                && dto.clientId().equals(CLIENT_ID)
                                                && dto.userAgent().equals(USER_AGENT)
                                                && dto.clientIp().equals(CLIENT_IP))))
                .thenReturn(RESPONSE);

        mockMvc.perform(
                        post("/api/auth/register")
                                .with(csrf())
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson.write(REQUEST).getJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(RESPONSE.accessToken())))
                .andExpect(jsonPath("$.refreshToken", is(RESPONSE.refreshToken())));

        verify(authService)
                .register(
                        argThat(
                                dto ->
                                        dto.email().equals(REQUEST.getEmail())
                                                && dto.password().equals(REQUEST.getPassword())
                                                && dto.clientId().equals(CLIENT_ID)
                                                && dto.userAgent().equals(USER_AGENT)
                                                && dto.clientIp().equals(CLIENT_IP)));
    }

    @Test
    @DisplayName("logout returns 200 OK and success message")
    void logoutReturnsSuccessMessage() throws Exception {
        mockClientIp();

        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(csrf())
                                .header("X-Refresh-Token", RESPONSE.refreshToken())
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Logout successful\"}"));

        verify(authService).logout(RESPONSE.refreshToken(), CLIENT_ID, USER_AGENT, CLIENT_IP);
    }

    @Test
    @DisplayName("exchangeToken returns 200 OK and authentication response")
    void exchangeTokenReturnsAuthenticationResponse() throws Exception {
        mockClientIp();

        Map<String, String> body = new HashMap<>();
        body.put("code", "auth-code");
        body.put("code_verifier", "code-verifier");
        body.put("redirect_uri", "http://localhost/callback");
        body.put("client_id", CLIENT_ID);

        when(authService.login(
                        argThat(
                                authCode ->
                                        authCode.getCode().equals(body.get("code"))
                                                && authCode.getCodeVerifier()
                                                        .equals(body.get("code_verifier"))
                                                && authCode.getRedirectUri()
                                                        .equals(body.get("redirect_uri"))
                                                && authCode.getClientId()
                                                        .equals(body.get("client_id"))
                                                && authCode.getUserAgent().equals(USER_AGENT)
                                                && authCode.getClientIp().equals(CLIENT_IP))))
                .thenReturn(RESPONSE);

        mockMvc.perform(
                        post("/api/auth/exchange")
                                .with(csrf())
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(RESPONSE.accessToken())))
                .andExpect(jsonPath("$.refreshToken", is(RESPONSE.refreshToken())));

        verify(authService)
                .login(
                        argThat(
                                authCode ->
                                        authCode.getCode().equals(body.get("code"))
                                                && authCode.getCodeVerifier()
                                                        .equals(body.get("code_verifier"))
                                                && authCode.getRedirectUri()
                                                        .equals(body.get("redirect_uri"))
                                                && authCode.getClientId()
                                                        .equals(body.get("client_id"))
                                                && authCode.getUserAgent().equals(USER_AGENT)
                                                && authCode.getClientIp().equals(CLIENT_IP)));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("getCurrentUser returns 200 OK and authenticated user")
    void getCurrentUserReturnsAuthenticatedUser() throws Exception {
        when(userPrincipal.getUsername()).thenReturn(REQUEST.getEmail());

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isOk());

        assertNotNull(userPrincipal);
        assertEquals(REQUEST.getEmail(), userPrincipal.getUsername());
    }

    @Test
    @DisplayName("health returns 200 OK and api online message")
    void healthReturnsApiOnlineMessage() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(content().string("API Online"));
    }
}
