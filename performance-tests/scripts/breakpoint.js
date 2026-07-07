import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { userJourney } from '../lib/user-journey.ts';

const USERS = new SharedArray('users', function () {
  return open('../data/users.csv').split('\n').slice(1).filter(Boolean).map(line => {
    const [email, pass] = line.split(',');
    return { email: email.trim(), pass: pass.trim() };
  });
});

function pickUser() {
  // VU-offset deterministic — no collisions
  return USERS[(__VU - 1 + __ITER * 5) % USERS.length];
}

export const options = {
  stages: [
    { duration: '5m', target: 120 },   // slow ramp to find the knee (2 CPU + cost 12)
  ],
  thresholds: {
    // Lenient — we WANT to find where it breaks
    // 5% failure rate signals the breakpoint
    'http_req_failed': ['rate<0.05'],
  },
};

export default function () {
  const user = pickUser();
  const result = userJourney(user);
  check(result, {
    'journey success': (r) => r.success,
  });
  sleep(1);
}
