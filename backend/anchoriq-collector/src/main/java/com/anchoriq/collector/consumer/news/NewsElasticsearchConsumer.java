package com.anchoriq.collector.consumer.news;

import com.anchoriq.collector.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 뉴스 이벤트 Consumer (Elasticsearch 저장).
 * news-events 토픽에서 뉴스를 수신하여 Elasticsearch news 인덱스에 저장한다.
 * Elasticsearch가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ElasticsearchOperations.class)
public class NewsElasticsearchConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewsElasticsearchConsumer.class);
    private static final String INDEX_NAME = "news";

    private final ElasticsearchOperations elasticsearchOperations;

    public NewsElasticsearchConsumer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.NEWS_EVENTS,
            groupId = "news-es-writer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(UUID.randomUUID().toString())
                    .withObject(message)
                    .build();

            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
            acknowledgment.acknowledge();
            log.debug("News event saved to Elasticsearch: {}", message.get("title"));
        } catch (Exception e) {
            log.error("Failed to save news to Elasticsearch: {}", e.getMessage());
            throw e;
        }
    }
}
