package com.anchoriq.api.controller.admin;

import com.anchoriq.api.application.datapipeline.DataPipelineApplicationService;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 데이터 파이프라인 관리 API 컨트롤러.
 * 수집기를 수동으로 즉시 실행하는 엔드포인트를 제공한다.
 * SecurityConfig에서 /api/admin/** hasRole ADMIN으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/data-pipeline")
@ConditionalOnBean(DataPipelineApplicationService.class)
@RequiredArgsConstructor
public class DataPipelineAdminController {

    private final DataPipelineApplicationService dataPipelineApplicationService;

    @PostMapping("/trigger/{source}")
    public ApiResponse<CollectorStatusDto> trigger(@PathVariable String source) {
        CollectorStatus status = dataPipelineApplicationService.triggerCollector(source);
        return ApiResponse.success(toDto(status));
    }

    @PostMapping("/trigger-all")
    public ApiResponse<List<CollectorStatusDto>> triggerAll() {
        List<CollectorStatusDto> results = dataPipelineApplicationService.triggerAll().stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.success(results);
    }

    private CollectorStatusDto toDto(CollectorStatus status) {
        return new CollectorStatusDto(
                status.name().value(),
                status.name().displayName(),
                status.enabled(),
                status.lastRunAt() != null ? status.lastRunAt().toString() : null,
                status.lastResult() != null ? status.lastResult().name() : null,
                status.schedule()
        );
    }

    public record CollectorStatusDto(
            String name,
            String displayName,
            boolean enabled,
            String lastRunAt,
            String lastResult,
            String schedule
    ) {
    }
}
