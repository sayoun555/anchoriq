package com.anchoriq.collector.source.ais;

import com.anchoriq.collector.producer.AisKafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AISstream.io WebSocket 클라이언트.
 * AIS 위치 데이터를 실시간 수신하여 Kafka ais-positions 토픽으로 전송한다.
 * 연결 끊김 시 자동 재연결한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class AisStreamWebSocketClient extends AbstractWebSocketHandler implements AisStreamClient {

    private static final Logger log = LoggerFactory.getLogger(AisStreamWebSocketClient.class);
    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final AisMessageParser messageParser;
    private final AisKafkaProducer aisKafkaProducer;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String websocketUrl;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession currentSession;

    public AisStreamWebSocketClient(
            AisMessageParser messageParser,
            AisKafkaProducer aisKafkaProducer,
            ObjectMapper objectMapper,
            String apiKey,
            String websocketUrl) {
        this.messageParser = messageParser;
        this.aisKafkaProducer = aisKafkaProducer;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.websocketUrl = websocketUrl;
    }

    @Override
    public void connect() {
        if (connected.get()) {
            log.info("AIS WebSocket already connected");
            return;
        }

        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            currentSession = client.execute(this, websocketUrl).get();
            log.info("AIS WebSocket connection established");
        } catch (Exception e) {
            log.error("Failed to connect to AIS WebSocket: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    @Override
    public void disconnect() {
        connected.set(false);
        reconnectScheduler.shutdown();
        if (currentSession != null && currentSession.isOpen()) {
            try {
                currentSession.close();
            } catch (Exception e) {
                log.warn("Error closing WebSocket session: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        connected.set(true);
        currentSession = session;
        sendSubscriptionMessage(session);
        log.info("AIS WebSocket connected, subscription sent");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        processPayload(message.getPayload());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String payload = new String(message.getPayload().array(), java.nio.charset.StandardCharsets.UTF_8);
        processPayload(payload);
    }

    private void processPayload(String payload) {
        try {
            Map<String, Object> parsed = messageParser.parse(payload);
            if (parsed != null) {
                String mmsi = messageParser.extractMmsi(parsed);
                aisKafkaProducer.send(mmsi, parsed);
            }
        } catch (Exception e) {
            log.warn("Error processing AIS message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connected.set(false);
        log.warn("AIS WebSocket disconnected: {}. Scheduling reconnect...", status);
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        connected.set(false);
        log.error("AIS WebSocket transport error: {}", exception.getMessage());
        scheduleReconnect();
    }

    @PreDestroy
    public void shutdown() {
        disconnect();
    }

    private void sendSubscriptionMessage(WebSocketSession session) throws Exception {
        Map<String, Object> subscription = Map.of(
                "APIKey", apiKey,
                "BoundingBoxes", List.of(
                        List.of(List.of(-90.0, -180.0), List.of(90.0, 180.0))
                )
        );
        String json = objectMapper.writeValueAsString(subscription);
        session.sendMessage(new TextMessage(json));
    }

    private void scheduleReconnect() {
        if (!reconnectScheduler.isShutdown()) {
            reconnectScheduler.schedule(this::connect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }
}
