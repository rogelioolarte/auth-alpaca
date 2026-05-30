import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '1s',
};

export default function () {
  const res = http.get('http://localhost:8080/api/auth');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is API Online': (r) => r.body === 'API Online',
  });
  sleep(1);
}
