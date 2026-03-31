import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * API 동시 요청 부하 테스트
 *
 * 시나리오:
 *   1. 선박 목록 조회 (GET /api/vessels)
 *   2. 리스크 스코어 조회 (GET /api/risk/score/vessel/{imo})
 *   3. AI 질의 (POST /api/ai/query)
 *   4. 글로벌 검색 (GET /api/search?q=)
 *
 * 200명 동시접속, stages 포함
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

const errorRate = new Rate('errors');
const vesselListTrend = new Trend('vessel_list_duration');
const riskScoreTrend = new Trend('risk_score_duration');
const aiQueryTrend = new Trend('ai_query_duration');
const searchTrend = new Trend('search_duration');

export const options = {
    scenarios: {
        vessel_list: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 80 },
                { duration: '2m', target: 80 },
                { duration: '30s', target: 0 },
            ],
            exec: 'vesselList',
        },
        risk_score: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '2m', target: 50 },
                { duration: '30s', target: 0 },
            ],
            exec: 'riskScore',
        },
        ai_query: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },
                { duration: '2m', target: 30 },
                { duration: '30s', target: 0 },
            ],
            exec: 'aiQuery',
        },
        global_search: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 40 },
                { duration: '2m', target: 40 },
                { duration: '30s', target: 0 },
            ],
            exec: 'globalSearch',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        errors: ['rate<0.01'],
        vessel_list_duration: ['p(95)<300'],
        risk_score_duration: ['p(95)<500'],
        ai_query_duration: ['p(95)<3000'],
        search_duration: ['p(95)<400'],
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

const SAMPLE_IMOS = [
    9321483, 9461867, 9502913, 9587079, 9632179,
    9702413, 9776171, 9839878, 9891901, 9912345,
];

export function vesselList() {
    const page = Math.floor(Math.random() * 10);
    const response = http.get(
        `${BASE_URL}/api/vessels?page=${page}&size=20`,
        authHeaders()
    );

    const success = check(response, {
        'vessel list: status 200': (r) => r.status === 200,
        'vessel list: response < 300ms': (r) => r.timings.duration < 300,
    });

    vesselListTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(1);
}

export function riskScore() {
    const imo = SAMPLE_IMOS[Math.floor(Math.random() * SAMPLE_IMOS.length)];
    const response = http.get(
        `${BASE_URL}/api/risk/score/vessel/${imo}`,
        authHeaders()
    );

    const success = check(response, {
        'risk score: status 200': (r) => r.status === 200,
        'risk score: response < 500ms': (r) => r.timings.duration < 500,
    });

    riskScoreTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(2);
}

export function aiQuery() {
    const queries = [
        'What is the risk level of vessels near Strait of Hormuz?',
        'Show me sanctioned vessels in the South China Sea',
        'Predict supply chain disruption for next week',
        'Which ports have the highest congestion?',
        'Analyze risk for IMO 9321483',
    ];
    const query = queries[Math.floor(Math.random() * queries.length)];

    const payload = JSON.stringify({ query: query });
    const response = http.post(
        `${BASE_URL}/api/ai/query`,
        payload,
        authHeaders()
    );

    const success = check(response, {
        'ai query: status 200': (r) => r.status === 200,
        'ai query: response < 3000ms': (r) => r.timings.duration < 3000,
    });

    aiQueryTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(3);
}

export function globalSearch() {
    const searchTerms = [
        'Evergreen', 'Maersk', 'COSCO', 'MSC', 'tanker',
        'Singapore', 'Rotterdam', 'Shanghai', 'Panama', 'crude oil',
    ];
    const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];

    const response = http.get(
        `${BASE_URL}/api/search?q=${encodeURIComponent(term)}`,
        authHeaders()
    );

    const success = check(response, {
        'search: status 200': (r) => r.status === 200,
        'search: response < 400ms': (r) => r.timings.duration < 400,
    });

    searchTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(1);
}
