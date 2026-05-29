# Performance Testing Report - Auth Alpaca

## 1. Introduction
This document outlines the strategy and planning for implementing performance tests for the Auth Alpaca backend. The goal is to evaluate the system's stability under expected loads and identify its breaking points through stress testing.

## 2. Objectives
- **Baseline Performance**: Establish a benchmark for response times and throughput under normal operating conditions.
- **Average Load Testing**: Validate that the system can handle the expected daily traffic without performance degradation.
- **Stress Testing**: Push the system beyond its limits to identify bottlenecks (CPU, Memory, DB connections, I/O) and observe the failure mode.
- **Stability/Soak Testing**: Ensure the system remains stable over an extended period under a consistent load.

## 3. Tooling & Infrastructure
- **Tool**: [k6](https://k6.io/)
- **Runtime**: JavaScript/TypeScript.
- **Execution Environment**: Kubernetes (aligned with the project's infrastructure).
- **Tests Location**: `/Performance-tests`

## 4. Endpoint Analysis & Load Distribution
Based on the architectural analysis, the following endpoints have been identified for testing. The "Suggested Load %" represents the proportion of total requests in a typical user session.

### Endpoint Mapping

| Controller | Method | Path | Description | Priority | Sug. Load % |
| :--- | :---: | :--- | :--- | :---: | :---: |
| **Auth** | `POST` | `/api/auth/login` | Authenticates user and returns tokens | **High** | 10% |
| | `POST` | `/api/auth/register` | Creates a new user account | Medium | 2% |
| | `POST` | `/api/auth/logout` | Invalidates current session/token | **High** | 5% |
| | `POST` | `/api/auth/exchange` | Exchanges auth code for tokens (OAuth/PKCE) | Medium | 3% |
| | `GET` | `/api/auth/me` | Retrieves current authenticated user details | **High** | 20% |
| | `GET` | `/api/auth` | Health check (API Online) | Low | 2% |
| **Refresh Token**| `POST` | `/api/auth/rotate` | Rotates the refresh token for a new access token | **High** | 25% |
| **Session** | `GET` | `/api/sessions/page` | Lists all active sessions for the user | Medium | 3% |
| | `DELETE` | `/api/sessions/{id}` | Revokes a specific active session | Medium | 2% |
| | `DELETE` | `/api/sessions/all` | Revokes all active sessions for the user | Medium | 1% |
| **User** | `GET` | `/api/users/{id}` | Retrieves a specific user's profile | Medium | 5% |
| | `POST` | `/api/users` | Admin: Creates a new user | Low | 1% |
| | `PUT` | `/api/users/{id}` | Admin: Updates user details | Low | 1% |
| | `DELETE` | `/api/users/{id}` | Admin/Self: Deletes a user account | Low | <1% |
| | `GET` | `/api/users` | Admin: Retrieves all users | Low | <1% |
| | `GET` | `/api/users/page` | Admin: Paginated list of users | Low | <1% |
| | `PUT` | `/api/users/change-password`| Updates user password | Medium | 2% |
| **Profile** | `GET` | `/api/profiles/{id}` | Retrieves a specific user profile | Medium | 5% |
| | `POST` | `/api/profiles` | Creates a new user profile | Medium | 2% |
| | `PUT` | `/api/profiles/{id}` | Updates an existing profile | Medium | 2% |
| | `DELETE` | `/api/profiles/{id}` | Deletes a profile | Low | <1% |
| | `GET` | `/api/profiles` | Admin: Retrieves all profiles | Low | <1% |
| | `GET` | `/api/profiles/page` | Admin: Paginated list of profiles | Low | <1% |
| **Advertiser** | `GET` | `/api/advertisers/{id}` | Retrieves advertiser details | Medium | 3% |
| | `POST` | `/api/advertisers` | Creates a new advertiser | Medium | 1% |
| | `PUT` | `/api/advertisers/{id}` | Updates advertiser details | Medium | 1% |
| | `DELETE` | `/api/advertisers/{id}` | Deletes an advertiser | Low | <1% |
| | `GET` | `/api/advertisers` | Admin: Retrieves all advertisers | Low | <1% |
| | `GET` | `/api/advertisers/page-admin`| Admin: Paginated advertisers | Low | <1% |
| | `GET` | `/api/advertisers/page` | Retrieves paginated list of indexed advertisers | Medium | 5% |
| **Role** | `GET` | `/api/roles/{id}` | Retrieves role details | Low | 1% |
| | `POST` | `/api/roles` | Creates a new role | Low | <1% |
| | `PUT` | `/api/roles/{id}` | Updates a role | Low | <1% |
| | `DELETE` | `/api/roles/{id}` | Deletes a role | Low | <1% |
| | `GET` | `/api/roles` | Retrieves all roles | Low | 1% |
| | `GET` | `/api/roles/page` | Paginated list of roles | Low | <1% |
| **Permission** | `GET` | `/api/permissions/{id}` | Retrieves permission details | Low | 1% |
| | `POST` | `/api/permissions` | Creates a new permission | Low | <1% |
| | `PUT` | `/api/permissions/{id}` | Updates a permission | Low | <1% |
| | `DELETE` | `/api/permissions/{id}` | Deletes a permission | Low | <1% |
| | `GET` | `/api/permissions` | Retrieves all permissions | Low | 1% |
| | `GET` | `/api/permissions/page` | Paginated list of permissions | Low | <1% |

## 5. Test Scenarios

### Scenario A: Average Load (Baseline)
- **Purpose**: Simulate the "normal" daily peak of users.
- **Profile**: Constant arrival rate of Virtual Users (VUs).
- **Duration**: 1 hour.
- **Target**: Ensure response times for High priority endpoints stay under 200ms.

### Scenario B: Stress Test (Breakpoint)
- **Purpose**: Find the maximum throughput (RPS) the system can handle before the error rate exceeds 1%.
- **Profile**: Ramp-up (e.g., from 0 to 1000 VUs over 10 minutes).
- **Duration**: Until failure.
- **Target**: Identify the resource that saturates first (CPU, DB Pool, etc.).

### Scenario C: Spike Test
- **Purpose**: Simulate sudden bursts of traffic (e.g., a marketing campaign or system reboot).
- **Profile**: Sudden jump from 10 VUs to 500 VUs in 30 seconds.
- **Duration**: Short bursts (5-10 minutes).
- **Target**: Test the effectiveness of the Rate Limiter and the system's ability to recover.

## 6. Success Metrics (KPIs)
| Metric | Target (Average Load) | Target (Stress) |
| :--- | :--- | :--- |
| **Response Time (p95)** | < 300ms | < 1s |
| **Error Rate** | < 0.1% | < 1% (up to breakpoint) |
| **CPU Utilization** | < 60% | < 90% |
| **DB Connection Pool** | < 50% usage | No exhaustion |

## 7. Implementation Roadmap
1. **Environment Setup**: Configure a dedicated performance environment mirroring production.
2. **Script Development**: Create k6 scripts in `/Performance-tests` using the endpoint map and load proportions.
3. **Dry Run**: Execute a low-VU test to verify script correctness.
4. **Execution**: Run Average $\rightarrow$ Spike $\rightarrow$ Stress scenarios.
5. **Analysis & Tuning**: Analyze results, tune JVM/DB parameters, and re-test.
