# Performance Test Suite

Validates the performance, stability, and capacity of the `auth-alpaca` auth system under resource-constrained conditions.

## Minimum Deployment Requirements

**The test suite and thresholds are calibrated for this minimum configuration:**

| Resource | Value | Notes |
|----------|-------|-------|
| CPU | **2 cores** | t3.micro / equivalent |
| RAM | **1 GB** | 750 MB heap (MaxRAMPercentage=75) |
| bcrypt cost | **12** | ~400 ms per hash on 2 cores |
| Tomcat threads | **8** | `server.tomcat.threads.max=8` |

See `docker-compose.yml` for the exact resource limits (`cpus: '2'`, `memory: 1G`) and JVM flags.

Running with more resources will pass all thresholds easily. Running with **less** will cause timeouts and false positives.

## Prerequisites

- [k6](https://k6.io/docs/getting-started/installation/) installed
- Docker Compose stack running (backend + PostgreSQL) with the minimum resources above
- Database seeded with test users (see below)

## Quick Start

```bash
# 1. Tune OS for concurrent connections
ulimit -n 65535

# 2. Seed 2000 test users into the database
psql -h 127.0.0.1 -U postgres -d auth-alpaca -f data/seeding-users.sql

# 3. Run a test (see Test Suite below for options)
k6 run scripts/baseline.js
```

The test outputs: `summary.html` (rich report), `summary.json` (raw data), and stdout (text summary).

## Test Suite

| Script | Type | Duration | VUs | What it measures |
|--------|------|----------|-----|-----------------|
| `smoke-test.js` | Sanity | ~2s | 1 | API responds "API Online" |
| `low-load-test.js` | Low load | 1.5 min | 15 | Behaviour at ~⅓ of capacity |
| `baseline.js` | Capacity + Stress | 10 min | 1→40→60 | Primary benchmark — see below |
| `breakpoint.js` | Limit finding | 5 min | 10→120 | Finds the exact knee where failure rate exceeds 5% |
| `spike.js` | Burst | 3 min | 15→30→80→30 | Recovery after sudden 2x traffic spike |
| `rate-limit.js` | Rate limit | 1 min | 1 | Verifies 500 RPM per-IP throttle |
| `registration.js` | Registration | 30s | 10 | Registration throughput (2x bcrypt cost per reg) |
| `isolated/` | Per-endpoint | varies | varies | Debug individual endpoints |

### Baseline (`baseline.js`)

The primary test, run after every meaningful change. Two sequential scenarios:

1. **Capacity** (0m–5m): Ramps from 1→40 VUs over 3 minutes, holds for 2. Measures the sustainable ceiling.
2. **Stress** (5m–10m): Ramps from 1→60 VUs over 3 minutes, holds for 2. Shows degradation past capacity.

Metrics are tagged by endpoint (`type:login`, `type:me`, `type:rotate`, `type:logout`).

### Thresholds

Calibrated for **2 CPU + bcrypt cost 12**:

| Test | Threshold | Rationale |
|------|-----------|-----------|
| **baseline** `{type:login}` | p(95) < 15s | bcrypt serialises on 2 cores |
| **baseline** `{type:me}` | p(95) < 8s | CPU starvation during bcrypt bursts |
| **baseline** `{type:rotate}` | p(95) < 6s | |
| **baseline** `{type:logout}` | p(95) < 6s | |
| **baseline** `http_req_failed` | < 1% | Non-negotiable |
| **low-load** `http_req_duration` | p(95) < 5s | Login dominates at low concurrency too |
| **low-load** `http_req_failed` | < 1% | |
| **breakpoint** `http_req_failed` | < 5% | Lenient — we WANT to find the breaking point |
| **spike** `http_req_duration` | p(95) < 25s | Queues spike during burst |
| **spike** `http_req_failed` | < 5% | Allows some timeouts during spike |
| **rate-limit** `http_req_failed` | 10% ≤ rate ≤ 95% | Must hit 429 (rate-limited), not all requests blocked |
| **registration** `http_req_failed` | < 5% | Each reg = 2 bcrypt ops (~800ms CPU) |

### When to run each test

| You want to... | Run |
|----------------|-----|
| Verify nothing is broken | `smoke-test.js` (CI) |
| Quick performance check | `low-load-test.js` (CI) |
| Full performance validation | `baseline.js` (pre-deploy) |
| Find the system's limit | `breakpoint.js` (capacity planning) |
| Test burst resilience | `spike.js` |
| Verify rate limiter | `rate-limit.js` |
| Test registration flow | `registration.js` |
| Debug a specific endpoint | `isolated/login.js`, etc. |

## User Selection

All tests use a **deterministic VU-offset formula** to pick users:

```
USERS[(__VU - 1 + __ITER * 5) % USERS.length]
```

This guarantees no two VUs collide on the same user concurrently, eliminating false login failures from session limits.

## Data

- `data/users.csv` — 2000 performance test users (email, password)
- `data/seeding-users.sql` — auto-generated SQL with bcrypt hashes for all 2000 users
- Users are distributed across VUs by the deterministic formula above. The 2000-user pool supports up to 200 VUs without wraparound collisions.

## OS Tuning

```bash
ulimit -n 65535
```

## Environment

`performance-tests/.env` contains:
- `BASE_URL` — backend URL (default: `http://localhost:8080`)
- `CLIENT_ID` — registered client UUID for auth headers
