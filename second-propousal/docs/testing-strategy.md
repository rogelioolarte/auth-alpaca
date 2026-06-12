# Testing Strategy

This document details the backend testing strategy, outlining the separation of Maven test profiles, integration testing with Testcontainers, and the performance testing setup using k6.

---

## 🧪 Maven Test Profiles Separation

To ensure fast feedback loops during development and comprehensive validation in CI/CD pipelines, tests are separated into two distinct Maven profiles.

### 1. Unit Testing Profile (`-Punit-tests`)
Focuses on fast, isolated checks (unit tests and slice tests) without database or external infrastructure dependencies.

* **Command**:
  ```bash
  ./mvnw clean test -Punit-tests
  ```
* **Execution details**:
  - Executes unit tests naming convention: `unit/**/*Test.java` via `maven-surefire-plugin`.
  - Disables the `maven-failsafe-plugin` execution (integration tests set to phase `none`).
  - JaCoCo plugin measures coverage and outputs standard unit test reports.

### 2. Integration Testing Profile (`-Pintegration-tests`)
Focuses on end-to-end functionality, HTTP request mapping, and database interaction tests.

* **Command**:
  ```bash
  ./mvnw clean verify -Pintegration-tests
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
An integration test class configures the database container dynamically:

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = 
        new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("auth-alpaca-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}
```

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
# Seed postgres container
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
