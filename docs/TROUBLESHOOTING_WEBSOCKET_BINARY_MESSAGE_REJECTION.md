# WebSocket 바이너리 메시지 1003 Close 무한 루프: AIS 수신 0 → 초당 70+ 메시지

## 배경 및 문제정의

`AisStreamWebSocketClient`가 AISstream.io에 연결하여 실시간 AIS 메시지를 수신하는 구조에서, 연결 성공 → 구독 메시지 전송 → 즉시 1003 Close → 재연결 → 반복하는 무한 루프가 발생했다. Kafka `ais-positions` 토픽에 메시지가 0건이었다.

Spring 로그에 `Standard WebSocket close: code=1003, reason=Binary messages not supported`가 반복 출력.

## 기술 선정 (대안 비교)

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| TextWebSocketHandler 유지 + 서버에 텍스트 요청 | 코드 변경 최소 | AISstream.io가 바이너리 전송을 제어 불가 | X |
| BinaryWebSocketHandler로 변경 | 바이너리 처리 가능 | 텍스트 메시지(구독 응답 등) 처리 불가 | X |
| AbstractWebSocketHandler로 변경 | 텍스트+바이너리 모두 처리 | 두 핸들러 메서드 구현 필요 | **O** |
| 외부 WebSocket 라이브러리 사용 | 유연한 프레임 처리 | Spring 생태계 이탈, 의존성 추가 | X |

## 분석

### 초기 판단

"서버가 연결을 거부하는 것 아닌가?" → 연결 자체는 성공(HTTP 101 Switching Protocols 확인). 구독 메시지 전송까지 성공. 그 직후 Close 프레임 수신.

### 실제 원인

`AisStreamWebSocketClient`가 `TextWebSocketHandler`를 상속하고 있었다. AISstream.io 서버는 데이터를 **바이너리 WebSocket 프레임(opcode 0x2)**으로 전송한다. `TextWebSocketHandler`는 `handleBinaryMessage`를 오버라이드하여 `CloseStatus(1003)` Close 프레임을 전송한다.

```java
// Spring TextWebSocketHandler 내부 구현
public class TextWebSocketHandler extends AbstractWebSocketHandler {
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);  // 1003 전송
        } catch (IOException ex) {
            // ...
        }
    }
}
```

### CS 원리: WebSocket 프로토콜 (RFC 6455)

**WebSocket 프레임 구조**: WebSocket은 TCP 위에 프레임 기반 프로토콜을 정의한다. 각 프레임의 첫 바이트에 opcode가 포함된다.

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+-------------------------------+
```

- **opcode 0x1 (Text Frame)**: 페이로드가 UTF-8 텍스트. 수신 측은 UTF-8 유효성을 검증해야 한다.
- **opcode 0x2 (Binary Frame)**: 페이로드가 임의의 바이트 시퀀스. 인코딩 검증 없음.
- **opcode 0x8 (Close Frame)**: 연결 종료. 페이로드 첫 2바이트가 상태 코드(빅엔디안).

**Close Code 1003 (NOT_ACCEPTABLE)**: RFC 6455 Section 7.4.1에 정의. "수신한 데이터 타입을 처리할 수 없음"을 의미. 텍스트만 지원하는 엔드포인트가 바이너리를 수신하거나 그 반대일 때 사용한다.

**Spring WebSocket 핸들러 계층**:

```
AbstractWebSocketHandler
├── handleTextMessage()      → 기본: 아무것도 안 함
├── handleBinaryMessage()    → 기본: 아무것도 안 함
└── handlePongMessage()      → 기본: 아무것도 안 함

TextWebSocketHandler extends AbstractWebSocketHandler
└── handleBinaryMessage()    → 오버라이드: session.close(1003)

BinaryWebSocketHandler extends AbstractWebSocketHandler
└── handleTextMessage()      → 오버라이드: session.close(1003)
```

`TextWebSocketHandler`는 "텍스트만 허용"이라는 의미를 핸들러 레벨에서 강제하는 설계다. 바이너리 수신 시 프로토콜 표준에 따라 1003을 보내는 것은 올바른 동작이지만, AISstream.io처럼 바이너리/텍스트 혼합 서버에는 부적합하다.

## 솔루션

`TextWebSocketHandler` → `AbstractWebSocketHandler`로 변경하고, 텍스트/바이너리 핸들러를 모두 구현.

```java
public class AisStreamWebSocketClient extends AbstractWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 구독 확인, 에러 메시지 등 텍스트 프레임 처리
        String payload = message.getPayload();
        processAisMessage(payload);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // AIS 위치 데이터 - 바이너리 프레임을 UTF-8 디코딩
        byte[] bytes = message.getPayload().array();
        String payload = new String(bytes, StandardCharsets.UTF_8);
        processAisMessage(payload);
    }

    private void processAisMessage(String payload) {
        try {
            AisStreamResponse response = objectMapper.readValue(payload, AisStreamResponse.class);
            if (response.getMessageType().equals("PositionReport")) {
                AisPositionMessage position = mapToPosition(response);
                kafkaTemplate.send("ais-positions",
                    String.valueOf(position.getMmsi()), position);
            }
        } catch (JsonProcessingException e) {
            log.warn("AIS 메시지 파싱 실패: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("AISstream 연결 성공, 구독 메시지 전송");
        String subscribeMsg = objectMapper.writeValueAsString(
            new AisSubscribeRequest(aisStreamApiKey, boundingBoxes));
        session.sendMessage(new TextMessage(subscribeMsg));
    }
}
```

## 결과 (Before / After)

| 지표 | Before | After |
|------|--------|-------|
| WebSocket 핸들러 | TextWebSocketHandler | AbstractWebSocketHandler |
| 바이너리 프레임 처리 | 1003 Close 전송 | UTF-8 디코딩 후 처리 |
| 연결 상태 | 연결→1003→재연결 무한 루프 | 안정적 장기 연결 유지 |
| AIS 메시지 수신 | 0 msg/s | 70+ msg/s |
| Kafka `ais-positions` | 0건 | 5분에 5,000+건 |
| 지도 선박 표시 | 0척 | 5,000+척 (5분 누적) |
