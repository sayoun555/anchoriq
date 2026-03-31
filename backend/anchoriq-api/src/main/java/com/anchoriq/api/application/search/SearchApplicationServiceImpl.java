package com.anchoriq.api.application.search;

import com.anchoriq.core.domain.maritime.ontology.service.OntologyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 글로벌 검색 Application Service 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchApplicationServiceImpl implements SearchApplicationService {

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int AUTOCOMPLETE_LIMIT = 10;

    private final OntologyDomainService ontologyDomainService;

    @Override
    public List<Map<String, Object>> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return ontologyDomainService.searchEntities(query.trim(), DEFAULT_SEARCH_LIMIT);
        } catch (Exception e) {
            log.warn("Search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> autocomplete(String query) {
        if (query == null || query.length() < 2) {
            return List.of();
        }
        try {
            return ontologyDomainService.searchEntities(query.trim(), AUTOCOMPLETE_LIMIT);
        } catch (Exception e) {
            log.warn("Autocomplete failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
