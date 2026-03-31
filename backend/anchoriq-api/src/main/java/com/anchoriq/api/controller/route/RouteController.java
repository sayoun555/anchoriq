package com.anchoriq.api.controller.route;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RouteController {

    private final RouteRepository routeRepository;
    private final ChokepointRepository chokepointRepository;

    @GetMapping("/routes")
    public ApiResponse<List<Route>> getRoutes() {
        return ApiResponse.success(routeRepository.findAll());
    }

    @GetMapping("/routes/{id}/chokepoints")
    public ApiResponse<List<Chokepoint>> getRouteChokepoints(@PathVariable Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id.toString()));
        return ApiResponse.success(route.getChokepoints().getValues());
    }

    @GetMapping("/chokepoints")
    public ApiResponse<List<Chokepoint>> getChokepoints() {
        try {
            List<Chokepoint> chokepoints = chokepointRepository.findAll();
            if (chokepoints == null) {
                return ApiResponse.success(Collections.emptyList());
            }
            return ApiResponse.success(chokepoints);
        } catch (Exception e) {
            log.warn("Failed to fetch chokepoints: {}", e.getMessage());
            return ApiResponse.success(Collections.emptyList());
        }
    }

    @GetMapping("/chokepoints/{name}")
    public ApiResponse<Chokepoint> getChokepointByName(@PathVariable String name) {
        Chokepoint chokepoint = chokepointRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Chokepoint", name));
        return ApiResponse.success(chokepoint);
    }
}
