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
    { duration: '30s', target: 15 },        // low load (~⅓ of capacity)
    { duration: '1m', target: 15 },
  ],
  thresholds: {
    // bcrypt cost 12 on 2 CPUs — even at low load login takes ~3s
    'http_req_duration': ['p(95)<5000'],     // 5s — dominated by login latency
    'http_req_failed':   ['rate<0.01'],      // <1% failures
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
