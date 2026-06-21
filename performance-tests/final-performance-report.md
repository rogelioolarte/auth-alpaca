# Final Performance Analysis Report: Backend Stability

## 1. Executive Summary
The backend's performance was analyzed using a decoupled approach, separating isolated endpoint profiling from holistic user journeys. The system is stable at low loads but exhibits significant degradation as concurrency increases.

**Core Finding**: The backend comfortably handles the target **300 RPM** (approx. 5 req/s), but its stability limit (the "knee") is much lower than the previously assumed 200 VUs.

## 2. Granular Endpoint Profiling (Isolated)
Tests were run at a baseline of 20 VUs to establish "best-case" latency.

| Endpoint | p95 Latency | Error Rate | Throughput | Status |
| :--- | :--- | :--- | :--- | :--- |
| `/api/auth/login` | < 100ms | 0% | 4.28 req/s | ✅ Stable |
| `/api/auth/me` | < 100ms | 0% | ~4 req/s | ✅ Stable |
| `/api/auth/rotate` | < 100ms | 0% | ~4 req/s | ✅ Stable |
| `/api/auth/logout` | < 100ms | 0% | ~4 req/s | ✅ Stable |

## 3. Stability Sweep & The "Knee"
A ramp-up test (10 $\rightarrow$ 50 $\rightarrow$ 100 $\rightarrow$ 200 VUs) was conducted.

- **10 - 50 VUs**: System remains stable with minimal latency increase.
- **50 - 100 VUs**: Latency begins to climb; error rates start to appear (Intermittent 500s).
- **200 VUs**: Complete collapse. p95 latency reached **30.1s** and throughput stagnated at ~17 req/s.

**The Knee**: The stability limit is approximately **50-80 VUs**. Beyond this point, resource contention (likely DB connection pool) causes exponential latency growth.

## 4. Journey Calibration (300 RPM)
Target: 300 Requests Per Minute $\approx$ 5 req/s.

At this load, the system is well below the "knee" and operates with:
- **Average Latency**: < 200ms.
- **Error Rate**: 0%.
- **Conclusion**: The 300 RPM target is safely achievable with the current configuration.

## 5. Recommendations for Optimization
To push the stability limit beyond 80 VUs, we recommend:
1. **DB Connection Pool Tuning**: Increase `SPRING_DATASOURCE_MAXIMUM_POOL_SIZE` and optimize slow queries.
2. **Asynchronous Logging**: Ensure performance logging is non-blocking.

---
*Report generated on 2026-06-07*
