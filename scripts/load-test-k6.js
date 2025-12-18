// k6 Load Testing Script for Mnemocast Engine API
// Usage: k6 run load-test-k6.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const playlistResponseTime = new Trend('playlist_response_time');
const analyticsResponseTime = new Trend('analytics_response_time');

export const options = {
  stages: [
    // Ramp-up: 0 to 100 users over 1 minute
    { duration: '1m', target: 100 },
    // Stay at 100 users for 2 minutes
    { duration: '2m', target: 100 },
    // Ramp-up to 500 users over 2 minutes
    { duration: '2m', target: 500 },
    // Stay at 500 users for 3 minutes
    { duration: '3m', target: 500 },
    // Ramp-up to 1000 users over 2 minutes
    { duration: '2m', target: 1000 },
    // Stay at 1000 users for 2 minutes
    { duration: '2m', target: 1000 },
    // Ramp-down to 0 users over 1 minute
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'], // 95% of requests should be below 200ms, 99% below 500ms
    http_req_failed: ['rate<0.01'], // Error rate should be less than 1%
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
  // Test 1: Health check (lightweight)
  const healthRes = http.get(`${BASE_URL}/api/v1/health`);
  check(healthRes, {
    'health check status is 200': (r) => r.status === 200,
    'health check response time < 50ms': (r) => r.timings.duration < 50,
  });
  errorRate.add(healthRes.status >= 400);

  sleep(0.1);

  // Test 2: Get playlist (most common operation)
  const screenId = `screen-${Math.floor(Math.random() * 9) + 1}`;
  const playlistRes = http.get(`${BASE_URL}/api/v1/screens/${screenId}/playlist?durationMinutes=3`);
  const playlistDuration = playlistRes.timings.duration;
  playlistResponseTime.add(playlistDuration);
  
  check(playlistRes, {
    'playlist status is 200': (r) => r.status === 200,
    'playlist response time < 100ms': (r) => r.timings.duration < 100,
    'playlist has items': (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.items && data.items.length > 0;
      } catch {
        return false;
      }
    },
  });
  errorRate.add(playlistRes.status >= 400);

  sleep(0.5);

  // Test 3: Analytics dashboard (heavier operation)
  if (Math.random() > 0.7) { // 30% of requests
    const analyticsRes = http.get(`${BASE_URL}/api/v1/analytics/dashboard`);
    const analyticsDuration = analyticsRes.timings.duration;
    analyticsResponseTime.add(analyticsDuration);
    
    check(analyticsRes, {
      'analytics status is 200': (r) => r.status === 200,
      'analytics response time < 200ms': (r) => r.timings.duration < 200,
    });
    errorRate.add(analyticsRes.status >= 400);
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
  };
}

function textSummary(data, options) {
  return `
========================================
Mnemocast Load Test Results
========================================
Duration: ${data.state.testRunDurationMs / 1000}s
VUs: ${data.metrics.vus.values.max}

HTTP Requests:
  Total: ${data.metrics.http_reqs.values.count}
  Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s
  Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
  Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
  Duration (p99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms
  Failed: ${data.metrics.http_req_failed.values.rate * 100}%

Custom Metrics:
  Playlist Response Time (avg): ${data.metrics.playlist_response_time.values.avg.toFixed(2)}ms
  Analytics Response Time (avg): ${data.metrics.analytics_response_time.values.avg.toFixed(2)}ms
  Error Rate: ${data.metrics.errors.values.rate * 100}%

Thresholds:
  ${data.metrics.http_req_duration.values['p(95)'] < 200 ? '✅' : '❌'} p95 < 200ms
  ${data.metrics.http_req_duration.values['p(99)'] < 500 ? '✅' : '❌'} p99 < 500ms
  ${data.metrics.http_req_failed.values.rate < 0.01 ? '✅' : '❌'} Error rate < 1%
========================================
  `;
}

