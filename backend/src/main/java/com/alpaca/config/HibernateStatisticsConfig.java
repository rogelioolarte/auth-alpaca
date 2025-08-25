package com.alpaca.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Configuration;

/**
 * Configures and enables Hibernate statistics for performance monitoring.
 *
 * <p>This configuration class activates Hibernate's internal statistics collection, which can be
 * useful for diagnosing performance issues, such as identifying slow queries or excessive database
 * hits. Enabling statistics allows access to detailed metrics about session activity, query
 * execution, cache usage, and more.
 *
 * <p>While Hibernate statistics are typically disabled in production environments due to their
 * potential impact on performance, they can be invaluable during development and testing phases. To
 * enable statistics in a Spring Boot application, set the following property in your {@code
 * application.properties} or {@code application.yml}:
 *
 * <pre>
 * spring.jpa.properties.hibernate.generate_statistics=true
 * </pre>
 *
 * <p>Additionally, to log detailed statistics, you can adjust the logging level:
 *
 * <pre>
 * logging.level.org.hibernate.stat=DEBUG
 * </pre>
 *
 * <p>For more information on enabling and using Hibernate statistics, refer to the following
 * resources:
 *
 * <ul>
 *   <li><a href="https://medium.com/@dixitsatish34/hibernate-statistics-5f7a5e1195ae">A beginner's
 *       guide to Hibernate Statistics</a>
 *   <li><a href="https://vladmihalcea.com/hibernate-statistics/">Hibernate Statistics</a>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class HibernateStatisticsConfig {

    /** Interface used to interact with the entity manager factory for the persistence unit. */
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Enables Hibernate statistics after the bean initialization.
     *
     * <p>This method unwraps the {@link EntityManagerFactory} to obtain the underlying {@link
     * SessionFactory}, then enables its statistics feature. This allows for the collection of
     * various performance metrics, such as query counts, cache hits, and session activity, which
     * can be useful for performance tuning and analysis.
     *
     * <p>Note: Enabling statistics can have a performance overhead. It is recommended to enable
     * them only in development or testing environments.
     */
    @PostConstruct
    public void enableStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        System.out.println("Hibernate Statistics enabled");
    }
}
