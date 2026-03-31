package com.anchoriq.api.controller.risk;

import com.anchoriq.api.application.timeline.TimelineApplicationService;
import com.anchoriq.api.dto.response.timeline.TimelineEventResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 타임라인 Controller — 이벤트-판단-액션 타임라인 조회.
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineApplicationService timelineService;

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> getTimeline(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TimelineEventResponse> events = timelineService.getTimeline(from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
