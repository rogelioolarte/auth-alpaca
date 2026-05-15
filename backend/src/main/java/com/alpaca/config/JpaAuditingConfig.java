package com.alpaca.config;

import com.alpaca.model.UserPrincipal;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
                return Optional.ofNullable(up.getUserId()).map(Object::toString);
            }

            return Optional.ofNullable(authentication.getName());
        };
    }
}
