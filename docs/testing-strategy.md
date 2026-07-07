# Testing Strategy

> 🏠 [README](../README.md) — **Testing Strategy**

## 📑 On This Page
- [🧪 Maven Test Profiles Separation](#-maven-test-profiles-separation)
- [🐳 Integration Testing with Testcontainers](#-integration-testing-with-testcontainers)
- [⚡ k6 Performance Testing Setup](#-k6-performance-testing-setup)

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

## ⚡ k6 Performance Testing Setup

The k6 test suite validates system capacity, API latency, and token rotation efficiency under concurrent loads.

### 1. OS Kernel Tuning
Before running high-load scenarios, the execution OS must be tuned to prevent socket exhaustion and limit issues:

```bash
# Increase maximum open file descriptors limit (connections count)
ulimit -n 65535
```

### 2. Database Seeding
To simulate real-world conditions and avoid database locking/contention on a single account, the database is pre-seeded with 2,000 unique users.

Seeding script run:
```bash
# Seed postgres container (adjust -h if the container is on a different host)
psql -h 127.0.0.1 -U postgres -d auth-alpaca -f performance-tests/data/seeding-users.sql
```

A mapping file (`performance-tests/data/users.csv`) containing these pre-seeded credentials is used in the k6 scripts via `SharedArray` to distribute credentials across virtual users (VUs).

### 3. Baseline Load Scenario (`baseline.js`)
The baseline test runs a typical production load scenario to measure latency characteristics.

* **Execution Command**:
  ```bash
  k6 run performance-tests/scripts/baseline.js
  ```

#### Virtual User (VU) Profile Configuration
The baseline script implements a 3-stage load curve:
```javascript
export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Stage 1: Warm-up phase
    { duration: '30s', target: 500 },  // Stage 2: Ramp up to baseline load
    { duration: '5m', target: 500 },   // Stage 3: Hold baseline load
  ],
  thresholds: {
    'http_req_duration': ['p(95)<300'], // 95% of requests must complete under 300ms
    'http_req_failed': ['rate<0.001'],  // Error rate must be less than 0.1%
  },
};
```

#### User Journey Execution Flow
Each virtual user executes a sequential user journey:
1. **Warm-up** (in warm-up stage): Performs basic login and profile fetch actions to prime the Spring Boot JIT compiler and Hikari Connection Pool.
2. **Standard Flow**:
   - **Login**: `POST /api/auth/login` (exchanges credentials for access/refresh token pair).
   - **Get Me**: `GET /api/auth/me` (fetches authenticated user principal details).
   - **Rotate**: `POST /api/auth/rotate` (rotates the token pair using the `X-Refresh-Token` header).
   - **Logout**: `POST /api/auth/logout` (revokes the current refresh token).
    - **Pacing**: `sleep(1)` (simulates user think time).

---

🏠 [Back to README](../README.md) | 📚 [Full Documentation](../README.md#-navigation-hub-docs-as-code)

#### Related Docs
- [Backend Architecture](backend-architecture.md) — Spring Boot API, JWT token system, and database schema
- [Deployment](deployment.md) — Docker Compose topology and environment configuration
