import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * Redis GEO 공간 검색 부하 테스트
 *
 * 시나리오: 근처 선박 검색 (GET /api/vessels/nearby?lat=&lon=&radius=)
 *
 * 전 세계 주요 해역 좌표를 기반으로 근처 선박을 검색하여
 * Redis GEORADIUS 성능을 검증한다.
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

const errorRate = new Rate('errors');
const geoSearchTrend = new Trend('geo_search_duration');

export const options = {
    stages: [
        { duration: '20s', target: 30 },
        { duration: '2m', target: 100 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<100'],   // Redis GEO should be very fast
        errors: ['rate<0.01'],
        geo_search_duration: ['p(95)<100'],
    },
};

function authHeaders() {
    return {
        headers: {
            'Content-Type': 'application/json',
            ...(AUTH_TOKEN ? { Authorization: `Bearer ${AUTH_TOKEN}` } : {}),
        },
    };
}

// Major maritime chokepoints and busy sea areas
const SEARCH_LOCATIONS = [
    { name: 'Strait of Malacca', lat: 1.43, lon: 103.75, radius: 50 },
    { name: 'Strait of Hormuz', lat: 26.57, lon: 56.25, radius: 30 },
    { name: 'Suez Canal', lat: 30.46, lon: 32.34, radius: 20 },
    { name: 'Panama Canal', lat: 9.08, lon: -79.68, radius: 20 },
    { name: 'Singapore Strait', lat: 1.27, lon: 103.85, radius: 30 },
    { name: 'English Channel', lat: 50.75, lon: 1.30, radius: 40 },
    { name: 'Bab el-Mandeb', lat: 12.58, lon: 43.33, radius: 30 },
    { name: 'Taiwan Strait', lat: 24.50, lon: 119.50, radius: 50 },
    { name: 'Busan Port', lat: 35.10, lon: 129.07, radius: 30 },
    { name: 'Rotterdam Port', lat: 51.95, lon: 4.13, radius: 30 },
    { name: 'Shanghai Port', lat: 31.23, lon: 121.47, radius: 40 },
    { name: 'Cape of Good Hope', lat: -34.36, lon: 18.50, radius: 60 },
    { name: 'Gulf of Aden', lat: 12.00, lon: 45.00, radius: 50 },
    { name: 'South China Sea', lat: 15.00, lon: 115.00, radius: 100 },
    { name: 'Mediterranean Sea', lat: 35.00, lon: 18.00, radius: 80 },
];

export default function () {
    const location = SEARCH_LOCATIONS[Math.floor(Math.random() * SEARCH_LOCATIONS.length)];

    // Add slight randomization to coordinates
    const lat = location.lat + (Math.random() - 0.5) * 2;
    const lon = location.lon + (Math.random() - 0.5) * 2;
    const radius = location.radius;

    const response = http.get(
        `${BASE_URL}/api/vessels/nearby?lat=${lat.toFixed(4)}&lon=${lon.toFixed(4)}&radius=${radius}`,
        authHeaders()
    );

    const success = check(response, {
        'geo search: status 200': (r) => r.status === 200,
        'geo search: response < 100ms': (r) => r.timings.duration < 100,
    });

    geoSearchTrend.add(response.timings.duration);
    errorRate.add(!success);

    sleep(0.5);
}
