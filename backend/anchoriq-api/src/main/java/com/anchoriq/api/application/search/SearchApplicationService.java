package com.anchoriq.api.application.search;

import java.util.List;
import java.util.Map;

/**
 * 글로벌 검색 Application Service 인터페이스.
 */
public interface SearchApplicationService {

    List<Map<String, Object>> search(String query);

    List<Map<String, Object>> autocomplete(String query);
}
