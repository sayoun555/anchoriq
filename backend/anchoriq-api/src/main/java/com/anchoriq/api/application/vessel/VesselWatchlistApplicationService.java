package com.anchoriq.api.application.vessel;

import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;

import java.util.List;

/**
 * 관심 선박 Application Service 인터페이스.
 */
public interface VesselWatchlistApplicationService {

    void addToWatchlist(Long userId, String vesselImo);

    void removeFromWatchlist(Long userId, String vesselImo);

    List<Watchlist> getWatchlist(Long userId);
}
