package com.anchoriq.api.application.news;

import com.anchoriq.api.dto.response.news.NewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 뉴스 Application Service 구현체.
 * Elasticsearch에서 뉴스를 검색한다.
 * Elasticsearch가 비활성화된 환경에서는 빈 목록을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsApplicationServiceImpl implements NewsApplicationService {

    @Nullable
    private final org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<NewsResponse> getNewsList(int page, int size) {
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch unavailable, returning empty news list");
            return Collections.emptyList();
        }
        // Elasticsearch 연동 시 실제 뉴스 인덱스에서 최신 순으로 조회한다.
        return Collections.emptyList();
    }

    @Override
    public List<NewsResponse> searchNews(String query, int page, int size) {
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch unavailable, returning empty search results");
            return Collections.emptyList();
        }
        // Elasticsearch 연동 시 multi_match 쿼리로 제목+본문을 검색한다.
        return Collections.emptyList();
    }
}
