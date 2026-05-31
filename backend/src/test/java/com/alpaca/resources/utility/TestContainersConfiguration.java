package com.alpaca.resources.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("sonar:S2187")
public class TestContainersConfiguration {

    @Value("${spring.datasource.database-name:auth_alpaca_test}")
    private String dbName;

    @Value("${spring.datasource.username:alpaca}")
    private String username;

    @Value("${spring.datasource.password:secret}")
    private String password;

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName(dbName)
                .withUsername(username)
                .withPassword(password);
    }
}
