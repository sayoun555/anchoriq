package com.anchoriq.api.application.news;

import com.anchoriq.api.dto.response.news.NewsResponse;

import java.util.List;

/**
 * 뉴스 Application Service 인터페이스.
 */
public interface NewsApplicationService {

    List<NewsResponse> getNewsList(int page, int size);

    List<NewsResponse> searchNews(String query, int page, int size);
}
