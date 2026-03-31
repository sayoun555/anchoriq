package com.anchoriq.api.controller.search;

import com.anchoriq.api.application.search.SearchApplicationService;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchApplicationService searchService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam String q) {
        return ApiResponse.success(searchService.search(q));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<Map<String, Object>>> autocomplete(@RequestParam String q) {
        return ApiResponse.success(searchService.autocomplete(q));
    }
}
