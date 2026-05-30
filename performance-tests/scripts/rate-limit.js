import { check, sleep } from 'k6';
import http from 'k6/http';
import { CONFIG } from '../lib/config.ts';

export const options = {
  vus: 1, // Single IP simulation
  duration: '1m',
};

export default function () {
  // Send requests as fast as possible to trigger the IPRateLimit
  const res = http.get(`${CONFIG.baseURL}/api/auth/me`, {
    headers: {
      'Authorization': 'Bearer invalid-token-to-test-rate-limit',
    },
  });

  check(res, {
    'is 429 Too Many Requests': (r) => r.status === 429,
  });

  // No sleep or very short sleep to maximize frequency
  sleep(0.1);
}
