package com.alpaca.config;

import com.alpaca.security.filter.JwtTokenValidatorFilter;
import com.alpaca.security.manager.CustomAuthenticationManager;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.oauth2.*;
import com.alpaca.service.IOAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures security settings for the application, including authentication, authorization, OAuth2
 * login, and JWT token validation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String O_AUTH_2_BASE_URI = "/oauth2/authorize";
    private static final String O_AUTH_2_REDIRECTION_END_POINT = "/oauth2/callback/*";

    @Value("${app.frontend.uri:http://localhost:4200}")
    private String allowedOrigin;

    private final JJwtManager jwtManager;
    private final IOAuth2Service securityService;
    private final AuthFailureHandler oauth2FailureHandler;
    private final AuthSuccessHandler oauth2SuccessHandler;
    private final CookieAuthReqRepo cookieAuthReqRepo;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomAuthenticationManager authenticationManager;

    /**
     * Builds the security filter chain with stateless session management, JWT validation, and
     * OAuth2 login support.
     *
     * <p>Configuration highlights:
     *
     * <ul>
     *   <li>CSRF, form login, and HTTP basic are disabled (stateless JWT auth).
     *   <li>Public endpoints: {@code /api/auth/**}, {@code /api/advertisers/**}, {@code
     *       /oauth2/**}.
     *   <li>Authenticated endpoints: {@code /api/sessions/**}, {@code /api/profiles/**}, {@code
     *       /api/users/**}.
     *   <li>Admin-only endpoints: {@code /api/roles/**}, {@code /api/permissions/**}.
     *   <li>All other requests are denied by default.
     *   <li>OAuth2 login is configured with cookie-based authorization request repository and a
     *       custom token response client.
     *   <li>A {@link JwtTokenValidatorFilter} is injected before {@link BasicAuthenticationFilter}
     *       to validate Bearer tokens on every request.
     * </ul>
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the built {@link SecurityFilterChain}
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(
                auth -> {
                    auth.requestMatchers("/api/auth/**", "/oauth2/**").permitAll();
                    auth.requestMatchers("/api/sessions/**").authenticated();
                    auth.requestMatchers("/api/advertisers/**").permitAll();
                    auth.requestMatchers("/api/profiles/**").authenticated();
                    auth.requestMatchers("/api/users/**").authenticated();
                    auth.requestMatchers("/api/roles/**").hasAnyRole("ADMIN");
                    auth.requestMatchers("/api/permissions/**").hasAnyRole("ADMIN");
                    auth.anyRequest().denyAll();
                });
        http.oauth2Login(
                oauth2 -> {
                    oauth2.authorizationEndpoint(
                            c -> {
                                c.baseUri(O_AUTH_2_BASE_URI);
                                c.authorizationRequestRepository(cookieAuthReqRepo);
                                c.authorizationRequestResolver(
                                        new OAuth2ReqResolver(
                                                clientRegistrationRepository, O_AUTH_2_BASE_URI));
                            });
                    oauth2.redirectionEndpoint(c -> c.baseUri(O_AUTH_2_REDIRECTION_END_POINT));
                    oauth2.userInfoEndpoint(c -> c.userService(securityService));
                    oauth2.tokenEndpoint(
                            c ->
                                    c.accessTokenResponseClient(
                                            authorizationCodeTokenResponseClient()));
                    oauth2.successHandler(oauth2SuccessHandler);
                    oauth2.failureHandler(oauth2FailureHandler);
                });
        http.exceptionHandling(
                exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(accessDeniedHandler())
                                .authenticationEntryPoint(authenticationEntryPoint()));
        http.authenticationProvider(authenticationManager);
        http.addFilterBefore(
                new JwtTokenValidatorFilter(jwtManager), BasicAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Creates the OAuth2 token response client used during the authorization code exchange.
     *
     * <p>A custom {@link AccessTokenResConverter} is plugged in to handle provider-specific token
     * response shapes. The rest client is configured with a {@link FormHttpMessageConverter} for
     * form-encoded responses and a custom {@link OAuth2ErrorResponseErrorHandler} for resilient
     * error handling.
     *
     * @return a fully configured OAuth2 token response client
     */
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            authorizationCodeTokenResponseClient() {
        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
                new OAuth2AccessTokenResponseHttpMessageConverter();
        tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(
                new AccessTokenResConverter());
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        RestClient restClient =
                RestClient.builder()
                        .configureMessageConverters(
                                builder ->
                                        builder.addCustomConverter(new FormHttpMessageConverter())
                                                .addCustomConverter(
                                                        tokenResponseHttpMessageConverter)
                                                .build())
                        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                        .build();
        client.setRestClient(restClient);
        return client;
    }

    /**
     * Returns an {@link AccessDeniedHandler} that responds with HTTP 403 and includes the access
     * denial reason in the response body.
     *
     * <p>This provides more diagnostic information than the default Spring Security 403 page.
     *
     * @return a handler that writes "403 Forbidden Access: {reason}" into the response
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (_, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter()
                    .write("403 Forbidden Access: " + accessDeniedException.getMessage());
        };
    }

    /**
     * Returns the {@link AuthenticationEntryPoint} used when an unauthenticated request reaches a
     * secured endpoint.
     *
     * <p>Delegates to Spring Security's {@link Http403ForbiddenEntryPoint}, which sends a plain 403
     * response. A dedicated entry point is defined here so that the exception-handling
     * configuration can be customized in one place if needed later.
     *
     * @return the entry point for unauthenticated access attempts
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new Http403ForbiddenEntryPoint();
    }

    /**
     * Exposes the default {@link AuthenticationManager} from Spring Security's {@link
     * AuthenticationConfiguration} as a bean.
     *
     * <p>This is necessary when custom authentication providers (like {@link
     * com.alpaca.security.manager.CustomAuthenticationManager}) are registered and the default
     * manager still needs to be available for injection elsewhere.
     *
     * @param configuration Spring's authentication configuration
     * @return the default {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager getJwtManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    /**
     * Defines the CORS configuration allowing the frontend origin to access the API.
     *
     * <p>The allowed origin is read from {@code app.frontend.uri} (default: {@code
     * http://localhost:4200}). Credentials are enabled for cookie-based flows (e.g., OAuth2 state
     * parameter cookies), and {@code maxAge} is set to 1 hour so browsers cache the preflight
     * result.
     *
     * @return a {@link CorsConfigurationSource} applied to all paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "Accept",
                        "X-Requested-With",
                        "remember-me",
                        "Authorization",
                        "X-Client-Id",
                        "User-Agent",
                        "X-Refresh-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
