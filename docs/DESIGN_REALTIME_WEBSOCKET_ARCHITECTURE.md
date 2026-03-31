## 실시간 WebSocket 아키텍처: 폴링 30초 지연 -> STOMP WebSocket 0.5초 실시간 전송

### 배경 및 문제정의
- 상황: AnchorIQ 대시보드는 (1) 선박 실시간 위치, (2) 리스크 알림, (3) 대시보드 통계를 실시간으로 갱신해야 한다. 사용자가 제재국 선박 이동을 즉시 인지해야 하는 보안 플랫폼 특성상, 지연은 곧 리스크이다.
- 문제: 기존 HTTP 폴링 방식은 클라이언트가 30초마다 GET 요청을 보내야 하고, 이벤트가 없을 때도 불필요한 요청이 발생한다. 100명 동시 접속 시 초당 3~4건의 불필요한 요청이 서버에 부하를 준다.

### 기술 선정 (대안 비교 테이블)

| 항목 | HTTP 폴링 (30초) | SSE (Server-Sent Events) | WebSocket (STOMP) |
|------|-----------------|------------------------|------------------|
| 지연 시간 | 최대 30초 | ~0.5초 (단방향) | ~0.5초 (양방향) |
| 양방향 통신 | 불가 | 불가 (서버->클라이언트만) | 가능 |
| 불필요한 요청 | 이벤트 없어도 발생 | 없음 | 없음 |
| 구독 관리 | 클라이언트가 관리 | 제한적 | STOMP 토픽 기반 |
| Spring 생태계 지원 | 기본 | SseEmitter | Spring WebSocket + STOMP |
| 브라우저 호환성 | 완벽 | IE 미지원 | SockJS 폴백으로 완벽 |

### 분석 / CS 원리

**WebSocket 프로토콜**: HTTP 핸드셰이크 후 TCP 연결을 업그레이드하여 전이중(full-duplex) 통신 채널을 유지한다. HTTP 폴링과 달리 연결 수립 오버헤드가 최초 1회만 발생한다.

**STOMP(Simple Text Oriented Messaging Protocol)**: WebSocket 위에 구독/발행 시맨틱을 추가하는 서브 프로토콜이다. 클라이언트가 `/topic/vessels/positions`를 구독하면, 서버가 해당 토픽에 메시지를 발행할 때만 전달된다. 토픽 기반 라우팅으로 불필요한 데이터 전송을 방지한다.

**SockJS 폴백**: WebSocket을 지원하지 않는 환경(프록시, 방화벽)에서 자동으로 Long Polling이나 iframe 기반 통신으로 폴백한다. 코드 변경 없이 호환성을 확보한다.

**3채널 분리 설계**:
- `/ws/vessels`: AIS 위치 데이터 스트림 (고빈도, 경량 데이터)
- `/ws/alerts`: 리스크 알림 (저빈도, 중요 이벤트)
- `/ws/dashboard`: 대시보드 통계 갱신 (주기적)

채널을 분리하면 클라이언트가 필요한 데이터만 구독할 수 있고, 각 채널의 QoS를 독립적으로 관리할 수 있다.

### 솔루션 (핵심 코드 블록)

```java
// WebSocket STOMP 설정 (Spring Boot)
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic prefix로 구독, /app prefix로 메시지 전송
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3개 WebSocket 엔드포인트
        registry.addEndpoint("/ws/vessels").setAllowedOrigins("*").withSockJS();
        registry.addEndpoint("/ws/alerts").setAllowedOrigins("*").withSockJS();
        registry.addEndpoint("/ws/dashboard").setAllowedOrigins("*").withSockJS();
    }
}
```

```java
// Kafka Consumer -> WebSocket 브릿지: 리스크 알림 실시간 전송
@Component
public class RiskAlertWebSocketConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "risk-alerts", groupId = "risk-alert-ws")
    public void consume(Map<String, Object> message, Acknowledgment ack) {
        // Kafka에서 수신한 리스크 알림을 WebSocket으로 실시간 전송
        messagingTemplate.convertAndSend("/topic/alerts", message);
        ack.acknowledge();
    }
}
```

```typescript
// React 프론트엔드: STOMP 구독
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
    webSocketFactory: () => new SockJS('/ws/vessels'),
    onConnect: () => {
        // 선박 위치 실시간 구독
        client.subscribe('/topic/vessels/positions', (message) => {
            const position = JSON.parse(message.body);
            updateVesselOnMap(position);
        });
    },
    // 연결 끊김 시 자동 재연결
    reconnectDelay: 5000,
});
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (HTTP 폴링 30초) | After (WebSocket STOMP) |
|------|------------------------|----------------------|
| 데이터 전달 지연 | 최대 30초 | ~0.5초 |
| 불필요한 HTTP 요청 (100명) | ~200 req/min | 0 (이벤트 기반) |
| 서버 부하 (동시 100명) | 매번 HTTP 핸드셰이크 | 영구 연결 유지 |
| 양방향 통신 | 불가 | 가능 (구독/해제 요청) |
| 네트워크 오버헤드 | HTTP 헤더 반복 | WebSocket 프레임 (2바이트) |
| 연결 끊김 복구 | 다음 폴링까지 대기 | SockJS 자동 재연결 5초 |
