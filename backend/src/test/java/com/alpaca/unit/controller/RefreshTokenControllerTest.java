package com.alpaca.unit.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.alpaca.controller.RefreshTokenController;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.utility.WithMockCustomUser;
import com.alpaca.security.ratelimit.IPRateLimit;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RefreshTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class RefreshTokenControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IRefreshTokenService service;

    @MockitoBean private IPRateLimit rateLimit;

    @MockitoBean private UserPrincipal userPrincipal;

    private MockedStatic<Utils> utilsMock;

    private static final String CLIENT_IP = "192.168.1.100";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String CLIENT_ID = "alpaca-client";
    private static final String USER_AGENT = "Mozilla/5.0";

    private static final AuthResponseDTO RESPONSE =
            new AuthResponseDTO("new-access-token", "new-refresh-token");

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
    @WithMockCustomUser
    @DisplayName("rotateRefreshToken returns 200 OK and rotated tokens")
    void rotateRefreshTokenReturnsRotatedTokens() throws Exception {
        mockClientIp();

        RateLimitResult allowedResult = new RateLimitResult(true, 0);

        when(rateLimit.check(CLIENT_IP)).thenReturn(allowedResult);

        when(service.rotateRefreshToken(REFRESH_TOKEN, CLIENT_ID, USER_AGENT, CLIENT_IP))
                .thenReturn(RESPONSE);

        mockMvc.perform(
                        post("/api/auth/rotate")
                                .with(csrf())
                                .principal(new TestingAuthenticationToken(userPrincipal, ""))
                                .header("X-Refresh-Token", REFRESH_TOKEN)
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(RESPONSE.accessToken())))
                .andExpect(jsonPath("$.refreshToken", is(RESPONSE.refreshToken())));

        verify(rateLimit).check(CLIENT_IP);

        verify(service).rotateRefreshToken(REFRESH_TOKEN, CLIENT_ID, USER_AGENT, CLIENT_IP);

        utilsMock.verify(() -> Utils.extractClientIP(any(HttpServletRequest.class)));
    }

    @Test
    @DisplayName("rotateRefreshToken returns 429 Too Many Requests when rate limit exceeded")
    void rotateRefreshTokenReturnsTooManyRequests() throws Exception {
        mockClientIp();

        RateLimitResult deniedResult = new RateLimitResult(false, 60);

        when(rateLimit.check(CLIENT_IP)).thenReturn(deniedResult);

        mockMvc.perform(
                        post("/api/auth/rotate")
                                .with(csrf())
                                .principal(new TestingAuthenticationToken(userPrincipal, ""))
                                .header("X-Refresh-Token", REFRESH_TOKEN)
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());

        verify(rateLimit).check(CLIENT_IP);

        verifyNoInteractions(service);

        utilsMock.verify(() -> Utils.extractClientIP(any(HttpServletRequest.class)));
    }

    @Test
    @DisplayName("rotateRefreshToken returns 401 Unauthorized when user is null")
    void rotateRefreshTokenReturnsUnauthorizedWhenUserIsNull() throws Exception {
        mockClientIp();

        RateLimitResult allowedResult = new RateLimitResult(true, 0);

        when(rateLimit.check(CLIENT_IP)).thenReturn(allowedResult);

        mockMvc.perform(
                        post("/api/auth/rotate")
                                .with(csrf())
                                .header("X-Refresh-Token", REFRESH_TOKEN)
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(rateLimit).check(CLIENT_IP);

        verifyNoInteractions(service);

        utilsMock.verify(() -> Utils.extractClientIP(any(HttpServletRequest.class)));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("rotateRefreshToken uses extracted client ip for rate limit and token rotation")
    void rotateRefreshTokenUsesExtractedClientIp() throws Exception {
        mockClientIp();

        RateLimitResult allowedResult = new RateLimitResult(true, 0);

        when(rateLimit.check(argThat(ip -> ip.equals(CLIENT_IP)))).thenReturn(allowedResult);

        when(service.rotateRefreshToken(
                        argThat(token -> token.equals(REFRESH_TOKEN)),
                        argThat(client -> client.equals(CLIENT_ID)),
                        argThat(agent -> agent.equals(USER_AGENT)),
                        argThat(ip -> ip.equals(CLIENT_IP))))
                .thenReturn(RESPONSE);

        mockMvc.perform(
                        post("/api/auth/rotate")
                                .with(csrf())
                                .principal(new TestingAuthenticationToken(userPrincipal, ""))
                                .header("X-Refresh-Token", REFRESH_TOKEN)
                                .header("X-Client-Id", CLIENT_ID)
                                .header("User-Agent", USER_AGENT))
                .andExpect(status().isOk());

        verify(rateLimit).check(CLIENT_IP);

        verify(service).rotateRefreshToken(REFRESH_TOKEN, CLIENT_ID, USER_AGENT, CLIENT_IP);

        utilsMock.verify(() -> Utils.extractClientIP(any(HttpServletRequest.class)));
    }
}
