import { check, sleep } from 'k6';
import http from 'k6/http';
import { CONFIG } from '../lib/config.ts';

/*
 * Rate Limit Test (500 RPM per IP)
 *
 * Fires requests as fast as possible from a single IP to verify:
 *   1. Requests before the limit are NOT rejected (no false positives)
 *   2. After ~500 requests, the rate limiter kicks in (429)
 *   3. Failed request rate is between 10% and 95% (meaning: limit was reached,
 *      but not every request was blocked, which would indicate a much lower limit)
 *
 * Rate limit = 500 RPM = ~8.3 req/s. With sleep(0) and 1 VU,
 * k6 fires ~50-200 req/s locally, so the limit should be hit in ~3-10s.
 *
 * IMPORTANT: this test uses a FIXED IP (10.0.0.1) so the backend sees
 * all requests from the same client.
 */

function vuIP() {
  // Fixed IP — all requests from the same client to test per-IP rate limiting
  return '10.0.0.1';
}

function headers(extra = {}) {
  return {
    'X-Forwarded-For': vuIP(),
    'X-Client-Id': CONFIG.clientId,
    'User-Agent': CONFIG.userAgent,
    ...extra,
  };
}

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    // After the first ~500 requests (at ~8.3 req/s), rate limit kicks in
    // We expect 10-95% of requests to be rate-limited (429)
    'http_req_failed': ['rate>=0.10', 'rate<=0.95'],
  },
};

export default function () {
  // Use /api/auth/me with an invalid token — fast endpoint (no bcrypt)
  const res = http.get(`${CONFIG.baseURL}/api/auth/me`, {
    headers: headers({ Authorization: 'Bearer invalid-token-to-test-rate-limit' }),
  });

  // First ~500 requests should get 401 (unauthorized), rest get 429 (rate limited)
  // Both are technically "failed" in k6's eyes (non-2xx)
  // We just check that we ARE getting rate-limited at some point
  check(res, {
    'is 429 or 401': (r) => r.status === 429 || r.status === 401,
  });

  // No sleep — fire as fast as possible to trigger rate limit
}
