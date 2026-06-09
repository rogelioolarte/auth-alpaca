# Initial Performance Limit Report

## Configuration
- **VU Count**: 200 Max VUs
- **Duration**: 3m 30s total
  - Warm-up: 30s ramp to 50 VUs
  - Ramp-up: 30s ramp to 200 VUs
  - Sustained: 2m at 200 VUs
- **Scenario**: Full User Journey (Login -> Get Me -> Rotate -> Logout) + 1s sleep.

## Metrics
| Metric | Value |
|--------|-------|
| Total RPS | 17.44 req/s |
| p95 Latency | 30.1s |
| p99 Latency | > 30.1s (max 43.82s) |
| Error Rate | 1.84% |
| Avg Iteration Duration | 34.5s |

## Analysis
The backend **does not handle 200 VUs comfortably**. 

While the total throughput (17.44 RPS) is higher than the absolute minimum target, the system exhibits severe performance degradation. The p95 latency of **30.1 seconds** indicates that a significant portion of users experience unacceptable delays. 

The average iteration duration (34.5s) compared to the expected minimal duration (roughly 4 requests + 1s sleep) shows that the system is likely bottlenecked by resource contention (e.g., database connection pool exhaustion, CPU saturation, or synchronous blocking calls).

## Calibration
- **Desired Target**: 200-300 RPM ($\approx 3.33$ to $5$ RPS).
- **Observed Capacity**: The system managed $\approx 17.4$ RPS, but with a complete collapse in latency.

**Conclusion**: Although the system can technically exceed the 5 RPS target in terms of raw throughput, it does so at the cost of stability and response time. The "comfortable" limit is likely much lower than 200 VUs. We should calibrate future tests to find the "knee" of the performance curve where latency begins to spike, as 200 VUs is already well into the saturation zone.
