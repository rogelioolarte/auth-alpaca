package com.alpaca.unit.security.filter;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.security.filter.JwtTokenValidatorFilter;
import com.alpaca.security.manager.JJwtManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link JwtTokenValidatorFilter}. */
@DisplayName("JwtTokenValidatorFilter Unit Tests")
class JwtTokenValidatorFilterTest {

    private static final class TestableJwtTokenValidatorFilter extends JwtTokenValidatorFilter {

        private TestableJwtTokenValidatorFilter(JJwtManager jwtManager) {
            super(jwtManager);
        }

        private void executeFilter(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    private JJwtManager jwtManager;
    private TestableJwtTokenValidatorFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtManager = mock(JJwtManager.class);
        filter = new TestableJwtTokenValidatorFilter(jwtManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("isAToken should return false when token is null")
    void isAToken_ShouldReturnFalse_WhenTokenIsNull() {
        boolean result = filter.isAToken(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAToken should return false when token does not start with bearer")
    void isAToken_ShouldReturnFalse_WhenTokenDoesNotStartWithBearer() {
        String token = "Basic token";

        boolean result = filter.isAToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAToken should return false when token length is insufficient")
    void isAToken_ShouldReturnFalse_WhenTokenLengthIsInsufficient() {
        String token = "Bearer short-token";

        boolean result = filter.isAToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("isAToken should return true when token is valid")
    void isAToken_ShouldReturnTrue_WhenTokenIsValid() {
        String token = "Bearer " + "a".repeat(121);

        boolean result = filter.isAToken(token);

        assertTrue(result);
    }

    @Test
    @DisplayName("doFilterInternal should authenticate user and continue filter chain")
    void doFilterInternal_ShouldAuthenticateUserAndContinueFilterChain()
            throws ServletException, IOException {

        String rawToken = "a".repeat(130);
        String authorizationHeader = "Bearer " + rawToken;

        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken("user", null);

        when(jwtManager.manageAuthentication(rawToken)).thenReturn(authenticationToken);

        filter.executeFilter(request, response, filterChain);

        assertSame(authenticationToken, SecurityContextHolder.getContext().getAuthentication());

        assertNotNull(authenticationToken.getDetails());

        verify(jwtManager).manageAuthentication(rawToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should throw exception when authentication token is null")
    void doFilterInternal_ShouldThrowException_WhenAuthenticationTokenIsNull()
            throws ServletException, IOException {

        String rawToken = "a".repeat(130);
        String authorizationHeader = "Bearer " + rawToken;

        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        when(jwtManager.manageAuthentication(rawToken)).thenReturn(null);

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> filter.executeFilter(request, response, filterChain));

        assertEquals("Invalid Access Token", exception.getReason());

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtManager).manageAuthentication(rawToken);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName(
            "doFilterInternal should continue filter chain when authorization header is missing")
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderIsMissing()
            throws ServletException, IOException {

        filter.executeFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verifyNoInteractions(jwtManager);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName(
            "doFilterInternal should continue filter chain when authorization header is invalid")
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderIsInvalid()
            throws ServletException, IOException {

        String invalidAuthorizationHeader = "Bearer short";

        request.addHeader(HttpHeaders.AUTHORIZATION, invalidAuthorizationHeader);

        filter.executeFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verifyNoInteractions(jwtManager);
        verify(filterChain).doFilter(request, response);
    }
}
