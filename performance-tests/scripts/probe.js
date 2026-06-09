import { check, sleep } from 'k6';
import { generateMarkdownSummary } from '../lib/reporting.ts';

export const options = {
  vus: 1,
  duration: '1s',
};

export default function () {
  // Just a dummy request to generate some metrics
  const res = __VU === 1 ? { status: 200, duration: 100 } : { status: 500, duration: 200 };
  sleep(0.1);
}

export function handleSummary(data) {
  const summary = generateMarkdownSummary('Probe Endpoint', data);
  return {
    'performance-tests/results/probe-report.md': summary,
    'stdout': summary,
  };
}
