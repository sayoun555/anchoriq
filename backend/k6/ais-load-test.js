import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * AIS 데이터 대량 유입 시뮬레이션
 *
 * 초당 수백 건의 AIS 위치 데이터를 POST하여
 * Kafka를 통한 AIS 이벤트 파이프라인의 처리 성능을 검증한다.
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');
const aisTrend = new Trend('ais_post_duration');

export const options = {
    stages: [
        { duration: '30s', target: 50 },   // ramp-up
        { duration: '2m', target: 200 },    // sustain: 200 VUs generating AIS data
        { duration: '30s', target: 0 },     // ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],   // p95 < 200ms
        errors: ['rate<0.01'],              // error rate < 1%
    },
};

function generateAisPayload() {
    const imo = 9000000 + Math.floor(Math.random() * 999999);
    const mmsi = 200000000 + Math.floor(Math.random() * 799999999);
    const lat = -90 + Math.random() * 180;
    const lon = -180 + Math.random() * 360;
    const speed = Math.random() * 25;
    const course = Math.random() * 360;
    const heading = Math.floor(Math.random() * 360);
    const timestamp = new Date().toISOString();

    return JSON.stringify({
        imo: imo,
        mmsi: mmsi,
        latitude: parseFloat(lat.toFixed(6)),
        longitude: parseFloat(lon.toFixed(6)),
        speed: parseFloat(speed.toFixed(1)),
        course: parseFloat(course.toFixed(1)),
        heading: heading,
        timestamp: timestamp,
        navigationStatus: 'UNDER_WAY',
        shipType: 'CARGO',
    });
}

export default function () {
    const payload = generateAisPayload();
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const response = http.post(`${BASE_URL}/api/ais/position`, payload, params);

    const success = check(response, {
        'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
        'response time < 200ms': (r) => r.timings.duration < 200,
    });

    aisTrend.add(response.timings.duration);
    errorRate.add(!success);

    sleep(0.01); // ~100 requests/s per VU
}
