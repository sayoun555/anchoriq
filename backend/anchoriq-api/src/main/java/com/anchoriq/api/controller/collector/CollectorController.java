package com.anchoriq.api.controller.collector;

import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CollectorController {

    @Nullable
    private final CollectorRegistry collectorRegistry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllStatuses() {
        if (collectorRegistry == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<Map<String, Object>> statuses = collectorRegistry.getAllStatuses().stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @PostMapping("/{name}/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> start(@PathVariable String name) {
        if (collectorRegistry == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UNAVAILABLE", "Kafka not connected"));
        }
        CollectorName collectorName = CollectorName.from(name);
        collectorRegistry.start(collectorName);
        return ResponseEntity.ok(ApiResponse.success(toMap(collectorRegistry.getStatus(collectorName))));
    }

    @PostMapping("/{name}/stop")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stop(@PathVariable String name) {
        if (collectorRegistry == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UNAVAILABLE", "Kafka not connected"));
        }
        CollectorName collectorName = CollectorName.from(name);
        collectorRegistry.stop(collectorName);
        return ResponseEntity.ok(ApiResponse.success(toMap(collectorRegistry.getStatus(collectorName))));
    }

    @PostMapping("/start-all")
    public ResponseEntity<ApiResponse<Void>> startAll() {
        if (collectorRegistry == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UNAVAILABLE", "Kafka not connected"));
        }
        collectorRegistry.startAll();
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/stop-all")
    public ResponseEntity<ApiResponse<Void>> stopAll() {
        if (collectorRegistry == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("UNAVAILABLE", "Kafka not connected"));
        }
        collectorRegistry.stopAll();
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Map<String, Object> toMap(CollectorStatus status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", status.name().value());
        map.put("displayName", status.name().displayName());
        map.put("enabled", status.enabled());
        map.put("schedule", status.schedule());
        map.put("lastRunAt", status.lastRunAt());
        map.put("lastResult", status.lastResult() != null ? status.lastResult().name() : null);
        return map;
    }
}
