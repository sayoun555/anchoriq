import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

/**
 * Neo4j 4홉 Cypher 쿼리 동시 요청 부하 테스트
 *
 * 시나리오:
 *   1. 온톨로지 그래프 확장 (GET /api/ontology/graph/{nodeId}/expand)
 *   2. 최단 경로 (GET /api/ontology/path)
 *   3. 제재 네트워크 (GET /api/ontology/sanctions/network)
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

const errorRate = new Rate('errors');
const graphExpandTrend = new Trend('graph_expand_duration');
const shortestPathTrend = new Trend('shortest_path_duration');
const sanctionsNetworkTrend = new Trend('sanctions_network_duration');

export const options = {
    scenarios: {
        graph_expand: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 20 },
                { duration: '2m', target: 50 },
                { duration: '20s', target: 0 },
            ],
            exec: 'graphExpand',
        },
        shortest_path: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 10 },
                { duration: '2m', target: 30 },
                { duration: '20s', target: 0 },
            ],
            exec: 'shortestPath',
        },
        sanctions_network: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 10 },
                { duration: '2m', target: 20 },
                { duration: '20s', target: 0 },
            ],
            exec: 'sanctionsNetwork',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        errors: ['rate<0.02'],
        graph_expand_duration: ['p(95)<1500'],
        shortest_path_duration: ['p(95)<2000'],
        sanctions_network_duration: ['p(95)<2000'],
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

const SAMPLE_NODE_IDS = [
    'vessel-9321483', 'vessel-9461867', 'vessel-9502913',
    'company-12345', 'company-67890',
    'port-KRPUS', 'port-SGSIN', 'port-NLRTM',
    'country-KR', 'country-CN', 'country-US',
];

export function graphExpand() {
    const nodeId = SAMPLE_NODE_IDS[Math.floor(Math.random() * SAMPLE_NODE_IDS.length)];
    const depth = Math.floor(Math.random() * 4) + 1; // 1~4 hops

    const response = http.get(
        `${BASE_URL}/api/ontology/graph/${encodeURIComponent(nodeId)}/expand?depth=${depth}`,
        authHeaders()
    );

    const success = check(response, {
        'graph expand: status 200': (r) => r.status === 200,
        'graph expand: response < 1500ms': (r) => r.timings.duration < 1500,
    });

    graphExpandTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(2);
}

export function shortestPath() {
    const fromIndex = Math.floor(Math.random() * SAMPLE_NODE_IDS.length);
    let toIndex = Math.floor(Math.random() * SAMPLE_NODE_IDS.length);
    while (toIndex === fromIndex) {
        toIndex = Math.floor(Math.random() * SAMPLE_NODE_IDS.length);
    }

    const from = SAMPLE_NODE_IDS[fromIndex];
    const to = SAMPLE_NODE_IDS[toIndex];

    const response = http.get(
        `${BASE_URL}/api/ontology/path?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
        authHeaders()
    );

    const success = check(response, {
        'shortest path: status 200': (r) => r.status === 200,
        'shortest path: response < 2000ms': (r) => r.timings.duration < 2000,
    });

    shortestPathTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(3);
}

export function sanctionsNetwork() {
    const entityIds = ['vessel-9321483', 'company-12345', 'country-KP', 'country-IR'];
    const entityId = entityIds[Math.floor(Math.random() * entityIds.length)];

    const response = http.get(
        `${BASE_URL}/api/ontology/sanctions/network?entityId=${encodeURIComponent(entityId)}&depth=3`,
        authHeaders()
    );

    const success = check(response, {
        'sanctions network: status 200': (r) => r.status === 200,
        'sanctions network: response < 2000ms': (r) => r.timings.duration < 2000,
    });

    sanctionsNetworkTrend.add(response.timings.duration);
    errorRate.add(!success);
    sleep(3);
}
