import { check, sleep } from 'k6';
import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { generateMarkdownSummary } from '../lib/reporting.ts';

const users = new SharedArray('users', function () {
   return open('../data/users.csv').split('\n').slice(1).map(line => {
    const parts = line.split(',');
    if (parts.length < 2) return null;
    return { email: parts[0].trim(), pass: parts[1].trim() };
  }).filter(Boolean);
});

export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '1m', target: 200 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  
  const loginRes = http.post('http://localhost:8080/api/auth/login', JSON.stringify({
    email: user.email,
    password: user.pass
  }), { headers: { 'Content-Type': 'application/json', 'X-Client-Id': 'performance-test-client' } });
  
  const token = loginRes.json().token;

  http.get('http://localhost:8080/api/auth/me', { headers: { 'Authorization': `Bearer ${token}` } });
  http.post('http://localhost:8080/api/auth/rotate', { headers: { 'Authorization': `Bearer ${token}` } });
  http.post('http://localhost:8080/api/auth/logout', { headers: { 'Authorization': `Bearer ${token}` } });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': generateMarkdownSummary('Stability Sweep', data),
  };
}
