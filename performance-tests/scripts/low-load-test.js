import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { userJourney } from '../lib/user-journey.ts';

const users = new SharedArray('users', function () {
   return open('../data/users.csv').split('\n').slice(1).map(line => {
    const parts = line.split(',');
    if (parts.length < 2) return null;
    return { email: parts[0].trim(), pass: parts[1].trim() };
  }).filter(Boolean);
});

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '1m', target: 100 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],
    'http_req_failed': ['rate<0.001'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  const result = userJourney(user);
  check(result, {
    'journey success': (r) => r.success,
  });
  sleep(1);
}
