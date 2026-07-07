import { check, sleep } from 'k6';
import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { generateMarkdownSummary } from '../../lib/reporting.ts';

const users = new SharedArray('users', function () {
   return open('../../data/users.csv').split('\n').slice(1).map(line => {
    const parts = line.split(',');
    if (parts.length < 2) return null;
    return { email: parts[0].trim(), pass: parts[1].trim() };
  }).filter(Boolean);
});

export const options = {
  stages: [
    { duration: '30s', target: 20 }, 
    { duration: '1m', target: 20 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  
  const res = http.post('http://localhost:8080/api/auth/login', JSON.stringify({
    email: user.email,
    password: user.pass
  }), { headers: { 'Content-Type': 'application/json', 'X-Client-Id': 'performance-test-client' } });

  check(res, { 'login success': (r) => r.status === 200 });
  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': generateMarkdownSummary('Login', data),
  };
}
