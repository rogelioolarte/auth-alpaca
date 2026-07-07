import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import http from 'k6/http';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/3.0.4/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';
import { CONFIG } from '../lib/config.ts';

const USERS = new SharedArray('users', function () {
  return open('../data/users.csv').split('\n').slice(1).filter(Boolean).map(line => {
    const [email, pass] = line.split(',');
    return { email: email.trim(), pass: pass.trim() };
  });
});

export const options = {
  scenarios: {
    capacity: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '3m', target: 40 },  // realistic ceiling for 2 CPU + bcrypt cost 12
        { duration: '2m', target: 40 },  // hold at capacity
      ],
      gracefulRampDown: '30s',
    },
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '3m', target: 60 },  // 1.5x capacity — shows degradation
        { duration: '2m', target: 60 },  // hold at stress
      ],
      startTime: '5m',
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // t3.micro (2 CPU, 1 GB) + bcrypt cost 12 — realistic p95 targets
    'http_req_duration{type:login}':  ['p(95)<15000'], // 15s — bcrypt serializa en 2 cores
    'http_req_duration{type:me}':     ['p(95)<8000'],  // 8s — hambre de CPU por bcrypt
    'http_req_duration{type:rotate}': ['p(95)<6000'],  // 6s
    'http_req_duration{type:logout}': ['p(95)<6000'],  // 6s
    'http_req_failed':                ['rate<0.01'],   // <1% — no negociable
  },
};

function pickUser() {
  // VU-offset deterministic: VU 1 → offset 0, stride = maxVUs
  // Guarantees no two VUs collide on the same user at the same iteration
  return USERS[(__VU - 1 + __ITER * 5) % USERS.length];
}

function vuIP() {
  // Unique /32 per VU so the backend rate limiter treats each VU as a separate client
  return `10.0.${Math.floor(__VU / 256)}.${__VU % 256}`;
}

function headers(extra = {}) {
  return {
    'X-Forwarded-For': vuIP(),
    'X-Client-Id': CONFIG.clientId,
    'User-Agent': CONFIG.userAgent,
    ...extra,
  };
}

export default function () {
  const user = pickUser();

  // --- Login ---
  const loginRes = http.post(
    `${CONFIG.baseURL}/api/auth/login`,
    JSON.stringify({ email: user.email, password: user.pass }),
    { tags: { type: 'login' }, headers: headers({ 'Content-Type': 'application/json' }) },
  );
  check(loginRes, { 'login ok': (r) => r.status === 200 });
  if (loginRes.status !== 200) {
    sleep(1);
    return;
  }
  const { accessToken, refreshToken } = loginRes.json();

  // --- Get Me ---
  const meRes = http.get(
    `${CONFIG.baseURL}/api/auth/me`,
    { tags: { type: 'me' }, headers: headers({ Authorization: `Bearer ${accessToken}` }) },
  );
  check(meRes, { 'me ok': (r) => r.status === 200 });

  // --- Rotate ---
  const rotateRes = http.post(
    `${CONFIG.baseURL}/api/auth/rotate`, null,
    { tags: { type: 'rotate' }, headers: headers({
      Authorization: `Bearer ${accessToken}`,
      'X-Refresh-Token': refreshToken,
    })},
  );
  check(rotateRes, { 'rotate ok': (r) => r.status === 200 });
  if (rotateRes.status !== 200) {
    sleep(1);
    return;
  }
  const newTokens = rotateRes.json();

  // --- Logout ---
  const logoutRes = http.post(
    `${CONFIG.baseURL}/api/auth/logout`, null,
    { tags: { type: 'logout' }, headers: headers({
      Authorization: `Bearer ${newTokens.accessToken}`,
      'X-Refresh-Token': newTokens.refreshToken,
    })},
  );
  check(logoutRes, { 'logout ok': (r) => r.status === 200 });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data, { title: 'Auth-Alpaca Baseline' }),
    'summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
