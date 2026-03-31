package com.anchoriq.api.controller.vessel;

import com.anchoriq.api.application.vessel.VesselWatchlistApplicationService;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vessels/watchlist")
@RequiredArgsConstructor
public class VesselWatchlistController {

    private final VesselWatchlistApplicationService watchlistService;

    @PostMapping
    public ApiResponse<Void> addToWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        String vesselImo = request.get("vesselImo");
        watchlistService.addToWatchlist(principal.userId(), vesselImo);
        return ApiResponse.success();
    }

    @DeleteMapping("/{imo}")
    public ApiResponse<Void> removeFromWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String imo) {
        watchlistService.removeFromWatchlist(principal.userId(), imo);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<Watchlist>> getWatchlist(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(watchlistService.getWatchlist(principal.userId()));
    }
}
