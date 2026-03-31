package com.anchoriq.collector.consumer.sanction;

import com.anchoriq.collector.config.KafkaTopicConfig;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 제재 업데이트 Consumer (Neo4j 저장).
 * sanction-updates 토픽에서 제재 정보를 수신하여 Neo4j Sanction 노드를 업데이트한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class SanctionNeo4jConsumer {

    private static final Logger log = LoggerFactory.getLogger(SanctionNeo4jConsumer.class);

    private final SanctionRepository sanctionRepository;

    public SanctionNeo4jConsumer(SanctionRepository sanctionRepository) {
        this.sanctionRepository = sanctionRepository;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.SANCTION_UPDATES,
            groupId = "sanction-neo4j-updater",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String action = String.valueOf(message.get("action"));
            String referenceNumber = String.valueOf(message.get("referenceNumber"));
            String targetName = String.valueOf(message.get("targetName"));

            if ("REMOVED".equalsIgnoreCase(action)) {
                sanctionRepository.findByReferenceNumber(referenceNumber)
                        .ifPresent(sanction -> {
                            sanction.deactivate();
                            sanctionRepository.save(sanction);
                        });
            } else {
                Sanction sanction = Sanction.create(
                        referenceNumber,
                        targetName,
                        String.valueOf(message.getOrDefault("targetType", "OTHER")),
                        "OpenSanctions",
                        LocalDate.now(),
                        null,
                        String.valueOf(message.getOrDefault("reason", ""))
                );
                sanctionRepository.save(sanction);
            }

            acknowledgment.acknowledge();
            log.debug("Sanction update processed: {} - {}", action, targetName);
        } catch (Exception e) {
            log.error("Failed to process sanction update: {}", e.getMessage());
            throw e;
        }
    }
}
