# Testing Strategy

> 🏠 [README](../README.md) — **Testing Strategy**

## 📑 On This Page
- [🧪 Maven Test Profiles Separation](#-maven-test-profiles-separation)
- [🐳 Integration Testing with Testcontainers](#-integration-testing-with-testcontainers)
- [⚡ k6 Performance Testing Suite](#-k6-performance-testing-suite)

---

This document details the backend testing strategy, outlining the separation of Maven test profiles, integration testing with Testcontainers, and the performance testing setup using k6.

---

## 🧪 Maven Test Profiles Separation

To ensure fast feedback loops during development and comprehensive validation in CI/CD pipelines, tests are separated into two distinct Maven profiles.

### 1. Unit Testing Profile (`-Punit-tests`)
Focuses on fast, isolated checks (unit tests and slice tests) without database or external infrastructure dependencies.

* **Command**:
  ```bash
  # From project root:
  ./backend/mvnw clean test -Punit-tests

  # Or from backend/ directly:
  cd backend && ./mvnw clean test -Punit-tests
  ```
* **Execution details**:
  - Executes unit tests naming convention: `unit/**/*Test.java` via `maven-surefire-plugin`.
  - Disables the `maven-failsafe-plugin` execution (integration tests set to phase `none`).
  - JaCoCo plugin measures coverage and outputs standard unit test reports.

### 2. Integration Testing Profile (`-Pintegration-tests`)
Focuses on end-to-end functionality, HTTP request mapping, and database interaction tests.

* **Command**:
  ```bash
  # From project root:
  ./backend/mvnw clean verify -Pintegration-tests

  # Or from backend/ directly:
  cd backend && ./mvnw clean verify -Pintegration-tests
  ```
* **Execution details**:
  - Executes integration tests naming convention: `integration/**/*IT.java` via `maven-failsafe-plugin`.
  - Excludes standard unit tests (`unit/**`).
  - Spins up containerized dependencies using **Testcontainers** (PostgreSQL).
  - JaCoCo generates a dedicated integration coverage report outputted to `${project.build.directory}/site-integration`.

---

## 🐳 Integration Testing with Testcontainers

Integration tests verify component interaction by launching real, lightweight instances of PostgreSQL in Docker containers before running tests.

### Configuration Template

The Testcontainers setup is centralized in a dedicated `@TestConfiguration` class, leveraging Spring Boot 3.1+ `@ServiceConnection` for automatic property registration:

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Value("${spring.datasource.database-name:auth_alpaca_test}")
    private String dbName;

    @Value("${spring.datasource.username:alpaca}")
    private String username;

    @Value("${spring.datasource.password:secret}")
    private String password;

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName(dbName)
                .withUsername(username)
                .withPassword(password);
    }
}
```

The base integration test class imports this configuration:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, JpaRepositoryConfig.class})
public abstract class BaseIntegrationTests {}
```

This approach avoids `@DynamicPropertySource` boilerplate and keeps container lifecycle management in a single location.

---

## ⚡ k6 Performance Testing Suite

The full k6 test suite, including thresholds, scenarios, environment setup, and calibration details, lives in **[`performance-tests/README.md`](../performance-tests/README.md)**.

Quick-start summary:

```bash
ulimit -n 65535                                           # OS tuning
psql -h 127.0.0.1 -U postgres -d auth-alpaca -f           # seed 2000 users
  performance-tests/data/seeding-users.sql
k6 run performance-tests/scripts/baseline.js               # capacity + stress
```

See the [performance test docs](../performance-tests/README.md) for the full test matrix, calibrated thresholds (2 CPU + bcrypt cost 12), and when to run each script.

---

🏠 [Back to README](../README.md) | 📚 [Full Documentation](../README.md#-navigation-hub-docs-as-code)

#### Related Docs
- [Backend Architecture](backend-architecture.md) — Spring Boot API, JWT token system, and database schema
- [Deployment](deployment.md) — Docker Compose topology and environment configuration
