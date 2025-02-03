package com.example.config;

import com.example.security.filter.JwtTokenValidatorFilter;
import com.example.security.manager.JJwtManager;
import com.example.security.manager.PasswordManager;
import com.example.security.oauth2.*;
import com.example.service.IAuthService;
import com.example.service.Impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String OAuth2BaseURI = "/oauth2/authorize";
    private static final String OAuth2RedirectionEndPoint = "/oauth2/callback/*";

    private final JJwtManager manager;
    private final PasswordManager passwordManager;
    private final AuthServiceImpl authService;
    private final AuthFailureHandler oauth2FailureHandler;
    private final AuthSuccessHandler oauth2SuccessHandler;
    private final CookieAuthReqRepo cookieAuthReqRepo;
    private final ClientRegistrationRepository clientRegistrationRepository;


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> {
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/auth/**", "/oauth2/**").permitAll();
            auth.requestMatchers("/api/profile/**").hasAnyRole("ADMIN", "USER");
            auth.requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER");
            auth.requestMatchers("/api/role/**").hasAnyRole("ADMIN");
            auth.requestMatchers("/api/permission/**").hasAnyRole("ADMIN");
            auth.anyRequest().denyAll();
        });
        http.oauth2Login(oauth2 -> {
            oauth2.authorizationEndpoint(c -> {
                c.baseUri(OAuth2BaseURI);
                c.authorizationRequestRepository(cookieAuthReqRepo);
                c.authorizationRequestResolver(
                        new OAuth2ReqResolver(clientRegistrationRepository, OAuth2BaseURI));
            });
            oauth2.redirectionEndpoint(
                    c -> c.baseUri(OAuth2RedirectionEndPoint));
            oauth2.userInfoEndpoint(
                    c -> c.userService(authService));
            oauth2.tokenEndpoint(c -> c
                    .accessTokenResponseClient(authorizationCodeTokenResponseClient()));
            oauth2.successHandler(oauth2SuccessHandler);
            oauth2.failureHandler(oauth2FailureHandler);
        });
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint(authenticationEntryPoint())
        );
        http.authenticationProvider(provider(authService));
        http.addFilterBefore(new JwtTokenValidatorFilter(manager), BasicAuthenticationFilter.class);
        return http.build();
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
    authorizationCodeTokenResponseClient() {
        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
                new OAuth2AccessTokenResponseHttpMessageConverter();
        tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(new AccessTokenResConverter());
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(RestClient.builder().messageConverters(
                        List.of(new FormHttpMessageConverter(), tokenResponseHttpMessageConverter))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build());
        return client;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response,
                accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("403 Forbidden Access: " + accessDeniedException.getMessage());

        };
    }
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new Http403ForbiddenEntryPoint();
    }

    @Bean
    public AuthenticationManager getManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider provider(IAuthService authService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordManager.passwordEncoder());
        provider.setUserDetailsService(authService);
        return provider;
    }

}
