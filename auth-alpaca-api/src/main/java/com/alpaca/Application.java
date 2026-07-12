package com.alpaca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Alpaca authentication service.
 *
 * <p>Bootstraps the Spring Boot application context. The {@code main} method is intentionally
 * package-private to prevent direct execution from external classes — the application is launched
 * through the standard {@code spring-boot-maven-plugin} or {@code java -jar} entry.
 */
@SpringBootApplication
public class Application {

    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments forwarded to {@link SpringApplication#run(Class,
     *     String[])}
     */
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
