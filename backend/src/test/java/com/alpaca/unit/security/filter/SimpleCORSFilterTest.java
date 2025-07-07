package com.alpaca.unit.security.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.security.filter.SimpleCORSFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for {@link SimpleCORSFilter}. */
@DisplayName("SimpleCORSFilter Unit Tests")
class SimpleCORSFilterTest {

  private SimpleCORSFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new SimpleCORSFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    chain = mock(FilterChain.class);
  }

  @Test
  @DisplayName("doFilter adds all CORS headers and continues the chain")
  void doFilterInternal_setsHeadersAndContinuesChain() throws ServletException, IOException {
    filter.doFilter(request, response, chain);

    assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
    assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    assertEquals(
        "POST, GET, PUT, OPTIONS, DELETE", response.getHeader("Access-Control-Allow-Methods"));
    assertEquals("3600", response.getHeader("Access-Control-Max-Age"));
    assertEquals(
        "Content-Type, Accept, X-Requested-With, remember-me, Authorization",
        response.getHeader("Access-Control-Allow-Headers"));

    // Verify filter chain is invoked exactly once
    verify(chain, times(1)).doFilter(request, response);
  }
}
