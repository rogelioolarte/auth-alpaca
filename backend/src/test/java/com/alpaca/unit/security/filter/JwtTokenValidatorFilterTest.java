package com.alpaca.unit.security.filter;

import com.alpaca.security.filter.JwtTokenValidatorFilter;
import com.alpaca.security.manager.JJwtManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link JwtTokenValidatorFilter}. */
@DisplayName("JwtTokenValidatorFilter Unit Tests")
class JwtTokenValidatorFilterTest {

    // Subclass to expose the protected doFilterInternal method
    private static class TestableFilter extends JwtTokenValidatorFilter {
        TestableFilter(JJwtManager manager) {
            super(manager);
        }

        public void invokeDoFilter(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
        }
    }

    private TestableFilter filter;
    private JJwtManager jwtManager;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtManager = mock(JJwtManager.class);
        filter = new TestableFilter(jwtManager);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("isAToken() tests")
    class IsATokenTests {
        @Test
        @DisplayName("Null header returns false")
        void nullHeader() {
            assertFalse(filter.isAToken(null));
        }

        @Test
        @DisplayName("Non-Bearer header returns false")
        void nonBearer() {
            assertFalse(filter.isAToken("Basic abcdef"));
        }

        @Test
        @DisplayName("Bearer but too short returns false")
        void bearerTooShort() {
            assertFalse(filter.isAToken("Bearer short"));
        }

        @Test
        @DisplayName("Bearer with sufficient length returns true")
        void bearerValidLength() {
            String longToken = "Bearer " + "x".repeat(121);
            assertTrue(filter.isAToken(longToken));
        }
    }

    @Nested
    @DisplayName("doFilterInternal() tests")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Valid token sets authentication and continues filter chain")
        void validToken_setsAuthentication() throws ServletException, IOException {
            String rawToken = "x".repeat(130);
            String bearer = "Bearer " + rawToken;
            request.addHeader(HttpHeaders.AUTHORIZATION, bearer);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("user", null, null);
            when(jwtManager.manageAuthentication(rawToken)).thenReturn(auth);

            filter.invokeDoFilter(request, response, chain);

            SecurityContext ctx = SecurityContextHolder.getContext();
            assertSame(auth, ctx.getAuthentication(), "Authentication should be set");

            assertNotNull(auth.getDetails(), "Details should be set from request");

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("Missing or invalid token does not set authentication, still continues chain")
        void invalidToken_doesNotSetAuthentication() throws ServletException, IOException {
            // No header
            filter.invokeDoFilter(request, response, chain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            // Invalid header
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer short");
            filter.invokeDoFilter(request, response, chain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verify(chain, times(2)).doFilter(request, response);
        }
    }
}
