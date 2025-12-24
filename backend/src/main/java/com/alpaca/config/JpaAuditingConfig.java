package com.alpaca.config;

import com.alpaca.model.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.TimeZone;

/**
 * Configuration class for enabling and customizing Spring Data JPA Auditing.
 *
 * <p>This class is responsible for two main tasks:
 *
 * <ul>
 *   <li>Enabling JPA Auditing across the application using {@code @EnableJpaAuditing}.
 *   <li>Defining a custom {@link AuditorAware} bean to automatically supply the identifier of the
 *       currently authenticated user for the auditing fields (e.g., createdBy, updatedBy) in the
 *       {@link com.alpaca.entity.Auditable} base class.
 * </ul>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /**
     * Creates an {@code AuditorAware<String>} bean that resolves the ID of the currently
     * authenticated user using Spring Security's context.
     *
     * <p>This implementation checks the security context:
     *
     * <ul>
     *   <li>If no user is authenticated, it returns an empty {@link Optional}.
     *   <li>If a user is authenticated, it casts the principal to {@link UserPrincipal} and returns
     *       the user's ID as a String.
     * </ul>
     *
     * @return An {@link AuditorAware} bean that provides the ID of the current auditor.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal up) {
                return Optional.ofNullable(up.getId()).map(Object::toString);
            }

            return Optional.ofNullable(authentication.getName());
        };
    }

    /**
     * Configures a {@link Jackson2ObjectMapperBuilder} bean used by Spring Boot to create and
     * customize the application's primary {@link ObjectMapper}.
     *
     * <p>This builder allows extending Spring Boot’s default Jackson configuration without
     * overriding it, ensuring compatibility with auto-registered modules, message converters, and
     * Spring MVC / WebFlux infrastructure.
     *
     * <p>The following customizations are applied:
     *
     * <ul>
     *   <li><b>JavaTimeModule:</b> Adds full support for Java 8+ Date/Time API classes (e.g.,
     *       {@code Instant}, {@code LocalDate}, {@code OffsetDateTime}).
     *   <li><b>WRITE_DATES_AS_TIMESTAMPS disabled:</b> Forces dates to be serialized in ISO-8601
     *       textual format instead of numeric epoch timestamps. Example:
     *       <pre>{@code "2025-12-07T22:10:00Z"}</pre>
     *       instead of:
     *       <pre>{@code 1733609400000}</pre>
     *   <li><b>UTC default timezone:</b> Ensures all serialized timestamps are produced in a
     *       consistent, unambiguous, globally interpretable timezone (UTC), which is a best
     *       practice for distributed applications and microservices.
     * </ul>
     *
     * <p>Because this bean customizes the builder rather than defining its own {@code
     * ObjectMapper}, Spring Boot will still apply:
     *
     * <ul>
     *   <li>auto-discovered Jackson modules
     *   <li>global properties under {@code spring.jackson.*}
     *   <li>default Spring MVC / WebFlux message-converter settings
     * </ul>
     *
     * <p>This approach is recommended by Spring Boot, as it allows augmenting Jackson behavior
     * without disabling or replacing the framework’s autoconfiguration.
     *
     * @return a configured {@link Jackson2ObjectMapperBuilder} bean used to build the application's
     *     primary {@link ObjectMapper}.
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .timeZone(TimeZone.getTimeZone("UTC"));
    }
}
