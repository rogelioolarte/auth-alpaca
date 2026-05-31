package com.alpaca.config;

import com.alpaca.repository.CustomJpaRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
@EnableJpaRepositories(
        basePackages = "com.alpaca.repository",
        repositoryBaseClass = CustomJpaRepositoryImpl.class)
public class JpaRepositoryConfig {}
