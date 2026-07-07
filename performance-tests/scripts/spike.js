import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { userJourney, warmup } from '../lib/user-journey';

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
    { duration: '30s', target: 15 },   // warm-up
    { duration: '1m',  target: 30 },   // near-capacity baseline
    { duration: '30s', target: 80 },   // spike: 2x capacity
    { duration: '1m',  target: 30 },   // recovery
  ],
  thresholds: {
    // During the spike, bcrypt queues up — p95 spikes to ~15-20s
    'http_req_duration': ['p(95)<25000'],  // 25s — allows spike degradation
    'http_req_failed':   ['rate<0.05'],     // 5% — spike tolerates some timeouts
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
