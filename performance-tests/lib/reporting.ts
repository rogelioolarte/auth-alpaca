export function generateMarkdownSummary(endpoint: string, data: any) {
  const p95 = data.metrics.http_req_duration?.values?.p95 || 0;
  const errorRate = data.metrics.http_req_failed?.values?.rate * 100 || 0;
  const throughput = data.metrics.http_reqs?.values?.rate || 0;
  const vus = data.metrics.vus?.values?.value || 0;

  return `### Endpoint: ${endpoint}
- **VUs**: ${vus}
- **p95 Latency**: ${p95.toFixed(2)}ms
- **Error Rate**: ${errorRate.toFixed(2)}%
- **Throughput**: ${throughput.toFixed(2)} req/s
`;
}
