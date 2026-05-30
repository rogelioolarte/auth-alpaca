import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { userJourney, warmup, User } from '../lib/user-journey';

const users = new SharedArray('users', function () {
  return JSON.parse(open('./data/users.csv').split('\n').slice(1).map(line => {
    const [email, password] = line.split(',');
    return { email, pass: password };
  }).filter(Boolean));
});

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Warm-up
    { duration: '1m', target: 500 },   // Baseline load
    { duration: '30s', target: 1500 }, // Spike: sudden burst to 1500 VUs
    { duration: '30s', target: 500 },  // Recovery: drop back to 500
    { duration: '2m', target: 500 },   // Stability after spike
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  
  if (k6.metrics.get('vus').values.length <= 50) {
    warmup(user);
  } else {
    const result = userJourney(user);
    check(result, {
      'journey success': (r) => r.success,
    });
  }
  sleep(1);
}
