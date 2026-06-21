package com.alpaca.config;

import com.alpaca.security.filter.JwtTokenValidatorFilter;
import com.alpaca.security.manager.CustomAuthenticationManager;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.oauth2.*;
import com.alpaca.service.IOAuth2Service;
import java.util.List;
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
     * Configures security filter chain with authentication and authorization settings.
     *
     * @param http HttpSecurity configuration.
     * @return SecurityFilterChain instance.
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
     * Creates an OAuth2 access token response client.
     *
     * @return OAuth2AccessTokenResponseClient instance.
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
     * Configures access denied handler.
     *
     * @return AccessDeniedHandler instance.
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
     * Configures an authentication entry point.
     *
     * @return AuthenticationEntryPoint instance.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new Http403ForbiddenEntryPoint();
    }

    /**
     * Retrieves the authentication manager from the configuration.
     *
     * @param configuration AuthenticationConfiguration instance.
     * @return AuthenticationManager instance.
     */
    @Bean
    public AuthenticationManager getJwtManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

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
