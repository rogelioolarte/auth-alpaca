package com.alpaca.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the Hibernate {@link SessionFactory} for programmatic statistics access.
 *
 * <p>This configuration unwraps the JPA {@link EntityManagerFactory} to obtain the underlying
 * Hibernate {@link SessionFactory} and stores a reference to its {@link Statistics} object. The
 * statistics themselves are controlled externally via the standard property {@code
 * spring.jpa.properties.hibernate.generate_statistics}.
 *
 * <p>Defining this bean in a {@code @Configuration} class ensures that {@link Statistics} can be
 * injected into monitoring or actuator components without coupling them to JPA bootstrap details.
 *
 * <p>When statistics are active (via the Hibernate property), the following metrics become
 * available:
 *
 * <ul>
 *   <li>Session open/close counts
 *   <li>Query execution counts and timings
 *   <li>Cache hit/miss ratios
 *   <li>Flush and transaction timings
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HibernateStatisticsConfig {

    /** Interface used to interact with the entity manager factory for the persistence unit. */
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Ensures the {@link SessionFactory} is accessible after bean initialization and logs that
     * statistics wiring is in place.
     *
     * <p>Note: Whether statistics are actually collected depends on the Hibernate configuration
     * property {@code hibernate.generate_statistics} — this method does not enable or disable them;
     * it only makes the statistics handle available for downstream use.
     */
    @PostConstruct
    public void enableStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(false);
        log.info("Hibernate Statistics enabled");
    }
}
