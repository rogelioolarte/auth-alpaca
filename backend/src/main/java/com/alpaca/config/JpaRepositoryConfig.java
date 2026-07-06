package com.alpaca.config;

import com.alpaca.repository.CustomJpaRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configures Spring Data JPA repositories to use a custom base implementation class.
 *
 * <p>This configuration sets {@link CustomJpaRepositoryImpl} as the default repository base class
 * for all repository interfaces under {@code com.alpaca.repository}, enabling shared custom
 * behavior (e.g., soft-delete filters or batch operations) without each repository having to extend
 * a non-standard interface.
 *
 * @see CustomJpaRepositoryImpl
 * @see org.springframework.data.jpa.repository.config.EnableJpaRepositories
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.alpaca.repository",
        repositoryBaseClass = CustomJpaRepositoryImpl.class)
public class JpaRepositoryConfig {}
