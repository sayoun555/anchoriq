package com.anchoriq.api.controller.admin;

import com.anchoriq.api.annotation.RequiresPlan;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 수집기 관리 API 컨트롤러.
 * 수집기별 on/off 제어 및 상태 조회 엔드포인트를 제공한다.
 * ENTERPRISE 플랜 이상에서만 접근 가능하다.
 * CollectorRegistry가 없는 환경(Kafka 비활성화)에서는 Bean이 생성되지 않는다.
 */
@RestController
@RequestMapping("/api/admin/collectors")
@ConditionalOnBean(CollectorRegistry.class)
@RequiredArgsConstructor
public class CollectorAdminController {

    private static final Logger log = LoggerFactory.getLogger(CollectorAdminController.class);

    private final CollectorRegistry collectorRegistry;

    @PostMapping("/{name}/start")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<CollectorStatusDto> start(@PathVariable String name) {
        CollectorName collectorName = CollectorName.from(name);
        collectorRegistry.start(collectorName);
        log.info("Admin: started collector [{}]", name);
        return ApiResponse.success(toDto(collectorRegistry.getStatus(collectorName)));
    }

    @PostMapping("/{name}/stop")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<CollectorStatusDto> stop(@PathVariable String name) {
        CollectorName collectorName = CollectorName.from(name);
        collectorRegistry.stop(collectorName);
        log.info("Admin: stopped collector [{}]", name);
        return ApiResponse.success(toDto(collectorRegistry.getStatus(collectorName)));
    }

    @GetMapping("/status")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<List<CollectorStatusDto>> getAllStatuses() {
        List<CollectorStatusDto> statuses = collectorRegistry.getAllStatuses().stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.success(statuses);
    }

    @GetMapping("/{name}/status")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<CollectorStatusDto> getStatus(@PathVariable String name) {
        CollectorName collectorName = CollectorName.from(name);
        return ApiResponse.success(toDto(collectorRegistry.getStatus(collectorName)));
    }

    @PostMapping("/start-all")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<List<CollectorStatusDto>> startAll() {
        collectorRegistry.startAll();
        log.info("Admin: started all collectors");
        List<CollectorStatusDto> statuses = collectorRegistry.getAllStatuses().stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.success(statuses);
    }

    @PostMapping("/stop-all")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<List<CollectorStatusDto>> stopAll() {
        collectorRegistry.stopAll();
        log.info("Admin: stopped all collectors");
        List<CollectorStatusDto> statuses = collectorRegistry.getAllStatuses().stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.success(statuses);
    }

    private CollectorStatusDto toDto(CollectorStatus status) {
        return new CollectorStatusDto(
                status.name().value(),
                status.enabled(),
                status.lastRunAt() != null ? status.lastRunAt().toString() : null,
                status.nextRunAt() != null ? status.nextRunAt().toString() : null,
                status.lastResult().name(),
                status.schedule()
        );
    }

    /**
     * 수집기 상태 응답 DTO.
     */
    public record CollectorStatusDto(
            String name,
            boolean enabled,
            String lastRunAt,
            String nextRunAt,
            String lastResult,
            String schedule
    ) {
    }
}
