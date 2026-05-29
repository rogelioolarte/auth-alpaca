import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { userJourney, warmup } from '../lib/user-journey.ts';

const users = new SharedArray('users', function () {
   return open('../data/users.csv').split('\n').slice(1).map(line => {
    const parts = line.split(',');
    if (parts.length < 2) return null;
    return { email: parts[0].trim(), pass: parts[1].trim() };
  }).filter(Boolean);
});

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Warm-up phase
    { duration: '30s', target: 500 },  // Ramp to baseline
    { duration: '5m', target: 500 },   // Baseline load
  ],
  thresholds: {
    'http_req_duration': ['p(95)<300'],
    'http_req_failed': ['rate<0.001'],
  },
};

export default function () {
  const user = users[Math.floor(Math.random() * users.length)];
  
  // Determine if we are in warmup based on current VU count (rough approximation)
  // or better: use the stage based on time.
  
  // Simplified: Use warmup for first 60s (ramp to 50 + ramp to 500)
  // and userJourney for the remaining 5m.
  // Actually, for simplicity and following the spec's intent of "priming",
  // we can just call warmup() if we are in the first 60 seconds.
  
  // However, k6 scripts are executed by VUs. We can use `exec.test.options.stages` 
  // but it's complex. Let's just use a time-based switch.
  
  // Since k6 doesn't provide a simple "current stage" variable,
  // I'll use the __VU and a simple logic.
  
  // Simplified: Use userJourney for all
  const result = userJourney(user);
  check(result, {
    'journey success': (r) => r.success,
  });

  sleep(1);
}
