package com.anchoriq.api.infrastructure.persistence.timeline;

import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;
import com.anchoriq.core.domain.operation.timeline.repository.TimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TimelineRepository Elasticsearch 구현체.
 * Elasticsearch가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchOperations.class)
public class TimelineRepositoryImpl implements TimelineRepository {

    private static final String INDEX_NAME = "timeline-events";

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void save(TimelineEvent event) {
        Map<String, Object> document = toDocument(event);
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(event.getId())
                .withObject(document)
                .build();
        elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
    }

    @Override
    public List<TimelineEvent> findByTimeRange(Instant from, Instant to, int page, int size) {
        Criteria criteria = new Criteria("timestamp")
                .greaterThanEqual(from.toString())
                .lessThanEqual(to.toString());

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));

        SearchHits<Map> hits = elasticsearchOperations.search(query, Map.class,
                IndexCoordinates.of(INDEX_NAME));

        return hits.getSearchHits().stream()
                .map(hit -> fromDocument(hit.getContent()))
                .toList();
    }

    @Override
    public List<TimelineEvent> findByRelatedEntityId(String entityId, int page, int size) {
        Criteria criteria = new Criteria("relatedEntityId").is(entityId);

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));

        SearchHits<Map> hits = elasticsearchOperations.search(query, Map.class,
                IndexCoordinates.of(INDEX_NAME));

        return hits.getSearchHits().stream()
                .map(hit -> fromDocument(hit.getContent()))
                .toList();
    }

    private Map<String, Object> toDocument(TimelineEvent event) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", event.getId());
        doc.put("eventType", event.getEventType());
        doc.put("source", event.getSource());
        doc.put("description", event.getDescription());
        doc.put("riskLevel", event.getRiskLevel() != null ? event.getRiskLevel().name() : null);
        doc.put("relatedEntityId", event.getRelatedEntityId());
        doc.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        return doc;
    }

    private TimelineEvent fromDocument(Map<String, Object> doc) {
        return TimelineEvent.builder()
                .id(getString(doc, "id"))
                .eventType(getString(doc, "eventType"))
                .source(getString(doc, "source"))
                .description(getString(doc, "description"))
                .riskLevel(parseRiskLevel(getString(doc, "riskLevel")))
                .relatedEntityId(getString(doc, "relatedEntityId"))
                .timestamp(parseInstant(getString(doc, "timestamp")))
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private RiskLevel parseRiskLevel(String level) {
        if (level == null || level.isBlank()) {
            return RiskLevel.LOW;
        }
        try {
            return RiskLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown risk level '{}', defaulting to LOW", level);
            return RiskLevel.LOW;
        }
    }

    private Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp '{}', using current time", timestamp);
            return Instant.now();
        }
    }
}
