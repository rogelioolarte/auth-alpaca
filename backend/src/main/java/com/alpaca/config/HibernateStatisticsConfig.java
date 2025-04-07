package com.alpaca.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateStatisticsConfig {

  private final EntityManagerFactory entityManagerFactory;

  @PostConstruct
  public void enableStatistics() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    System.out.println("Hibernate Statistics enabled");
  }
}
