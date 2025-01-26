package com.example.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateStatisticsConfig {

    private final EntityManagerFactory entityManagerFactory;

//    @PostConstruct
//    public void enableStatistics() {
//        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
//        Statistics stats = sessionFactory.getStatistics();
//        stats.setStatisticsEnabled(true);
//        System.out.println("Hibernate Statistics enabled");
//    }
}
