package com.alpaca.security.filter;

import com.alpaca.security.manager.JJwtManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenValidatorFilter extends OncePerRequestFilter {

  private final JJwtManager manager;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (isAToken(jwtToken)) {
      SecurityContext context = SecurityContextHolder.getContext();
      UsernamePasswordAuthenticationToken authentication =
          manager.manageAuthentication(jwtToken.substring(7));
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
    }
    filterChain.doFilter(request, response);
  }

  public boolean isAToken(String token) {
    return token != null && token.startsWith("Bearer ") && token.length() > 120;
  }
}
