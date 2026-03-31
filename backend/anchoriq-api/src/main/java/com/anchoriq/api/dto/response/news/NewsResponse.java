package com.anchoriq.api.dto.response.news;

import lombok.Builder;
import lombok.Getter;

/**
 * 뉴스 응답 DTO.
 */
@Getter
@Builder
public class NewsResponse {

    private final String id;
    private final String title;
    private final String summary;
    private final String source;
    private final String url;
    private final String publishedAt;
    private final String category;
}
