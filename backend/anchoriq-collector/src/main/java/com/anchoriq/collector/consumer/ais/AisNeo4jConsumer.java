package com.anchoriq.collector.consumer.ais;

import com.anchoriq.collector.config.KafkaTopicConfig;
import com.anchoriq.core.domain.maritime.vessel.model.Flag;
import com.anchoriq.core.domain.maritime.vessel.model.Imo;
import com.anchoriq.core.domain.maritime.vessel.model.Mmsi;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselStatus;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIS 위치 데이터 Consumer (Neo4j 상태 배치 업데이트).
 *
 * 설계: KAFKA_DESIGN.md — "ais-neo4j-updater: Consumer 1개 (배치 처리, 10초마다 모아서)"
 *
 * 초당 수백 건의 AIS 메시지를 건건이 Neo4j에 쓰면 커넥션 풀이 고갈된다.
 * 대신 메모리 버퍼에 최신 상태만 유지하고, 10초마다 변경된 선박만 배치로 업데이트한다.
 * 같은 MMSI의 상태가 여러 번 오면 마지막 것만 적용 (최종 일관성).
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class AisNeo4jConsumer {

    private static final Logger log = LoggerFactory.getLogger(AisNeo4jConsumer.class);

    private final VesselRepository vesselRepository;
    private final ConcurrentHashMap<String, Map<String, Object>> pendingUpdates = new ConcurrentHashMap<>();

    public AisNeo4jConsumer(VesselRepository vesselRepository) {
        this.vesselRepository = vesselRepository;
    }

    /**
     * Kafka에서 AIS 메시지를 수신하여 메모리 버퍼에 최신 상태를 저장한다.
     * DB 쓰기 없이 즉시 반환 → 초당 수백 건 처리 가능.
     */
    @KafkaListener(
            topics = KafkaTopicConfig.AIS_POSITIONS,
            groupId = "ais-neo4j-updater",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String mmsi = String.valueOf(message.get("mmsi"));

            if (mmsi != null && !mmsi.isBlank()) {
                pendingUpdates.put(mmsi, message);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to buffer AIS message: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 10초마다 버퍼의 변경 사항을 Neo4j에 배치 업데이트한다.
     * 같은 MMSI가 여러 번 들어왔어도 마지막 상태만 적용.
     * Neo4j에 없는 선박은 AIS 데이터 기반으로 자동 생성한다.
     */
    @Scheduled(fixedDelay = 10_000)
    public void flushToNeo4j() {
        if (pendingUpdates.isEmpty()) return;

        Map<String, Map<String, Object>> batch = new ConcurrentHashMap<>(pendingUpdates);
        pendingUpdates.clear();

        int updated = 0;
        int created = 0;
        int failed = 0;

        for (Map.Entry<String, Map<String, Object>> entry : batch.entrySet()) {
            try {
                String mmsi = entry.getKey();
                Map<String, Object> msg = entry.getValue();
                VesselStatus newStatus = parseStatus(String.valueOf(msg.getOrDefault("status", "UNKNOWN")));

                var existing = vesselRepository.findByMmsi(mmsi);
                if (existing.isPresent()) {
                    Vessel vessel = existing.get();
                    vessel.updateStatusFromAis(newStatus);
                    vesselRepository.save(vessel);
                    updated++;
                } else {
                    String rawImo = String.valueOf(msg.getOrDefault("imo", ""));
                    String name = String.valueOf(msg.getOrDefault("name", "UNKNOWN"));
                    String rawFlag = String.valueOf(msg.getOrDefault("flag", ""));
                    VesselType type = resolveVesselType(String.valueOf(msg.getOrDefault("type", "OTHER")));

                    Vessel vessel = Vessel.builder()
                            .imo(isValidImo(rawImo) ? Imo.of(rawImo) : null)
                            .mmsi(Mmsi.of(mmsi))
                            .name(name)
                            .flag(isNotBlank(rawFlag) ? Flag.of(rawFlag) : null)
                            .type(type)
                            .build();
                    vessel.updateStatusFromAis(newStatus);
                    vesselRepository.save(vessel);
                    created++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to update vessel {}: {}", entry.getKey(), e.getMessage());
            }
        }

        if (updated > 0 || created > 0 || failed > 0) {
            log.info("Neo4j batch update: {} updated, {} created, {} failed (from {} buffered)",
                    updated, created, failed, batch.size());
        }
    }

    private VesselStatus parseStatus(String status) {
        try {
            return VesselStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return VesselStatus.UNKNOWN;
        }
    }

    private VesselType resolveVesselType(String type) {
        try {
            return VesselType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return VesselType.OTHER;
        }
    }

    private boolean isValidImo(String imo) {
        return imo != null && !imo.isBlank() && imo.matches("\\d{7}");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
