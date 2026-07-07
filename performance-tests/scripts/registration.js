import { check, sleep } from 'k6';
import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { CONFIG } from '../lib/config.ts';

/*
 * Registration Test
 *
 * Registration uses bcrypt to hash the password (~400ms CPU with cost 12)
 * PLUS a login (~400ms) = 2x CPU cost of a normal login.
 * With 2 CPUs, sustainable registration throughput is ~2.5 reg/s.
 *
 * At 10 VUs each doing register + sleep(1), that's ~5 reg/s — pushing
 * the limit. We keep it short (30s) to limit database growth.
 */

const usedEmails = new Set();

function uniqueEmail() {
  // Guaranteed unique per VU+ITER across all runs
  return `reg_${__VU}_${__ITER}_${Date.now()}@perftest.local`;
}

export const options = {
  stages: [
    { duration: '15s', target: 10 },
    { duration: '15s', target: 10 },
  ],
  thresholds: {
    // Registration includes 2 bcrypt ops — some may time out under load
    'http_req_failed': ['rate<0.05'],
  },
};

export default function () {
  const payload = JSON.stringify({
    email: uniqueEmail(),
    password: 'Password123!',
    clientId: CONFIG.clientId,
  });

  const res = http.post(`${CONFIG.baseURL}/api/auth/register`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'registration ok': (r) => r.status === 200,
  });

  sleep(1);
}
