import { check, sleep } from 'k6';
import http from 'k6/http';
import { CONFIG } from '../lib/config.ts';

export const options = {
  vus: 100,
  duration: '2m',
};

export default function () {
  // Generate unique user data for registration
  const uniqueId = `${__VU}-${__ITER}`;
  const payload = JSON.stringify({
    email: `reg_test_${uniqueId}@example.com`,
    password: 'Password123!',
    clientId: CONFIG.clientId,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(`${CONFIG.baseURL}/api/auth/register`, payload, params);

  check(res, {
    'registration successful': (r) => r.status === 201 || r.status === 200,
  });

  sleep(1);
}
