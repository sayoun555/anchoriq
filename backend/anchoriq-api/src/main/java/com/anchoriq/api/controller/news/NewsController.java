package com.anchoriq.api.controller.news;

import com.anchoriq.api.application.news.NewsApplicationService;
import com.anchoriq.api.dto.response.news.NewsResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 뉴스 조회 Controller.
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsApplicationService newsApplicationService;

    @GetMapping
    public ApiResponse<List<NewsResponse>> getNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(newsApplicationService.getNewsList(page, size));
    }

    @GetMapping("/search")
    public ApiResponse<List<NewsResponse>> searchNews(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(newsApplicationService.searchNews(q, page, size));
    }
}
