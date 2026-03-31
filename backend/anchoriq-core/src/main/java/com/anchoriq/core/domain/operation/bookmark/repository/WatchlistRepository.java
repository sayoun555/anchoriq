package com.anchoriq.core.domain.operation.bookmark.repository;

import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;

import java.util.List;

/**
 * 관심 선박 Repository 인터페이스.
 */
public interface WatchlistRepository {

    Watchlist save(Watchlist watchlist);

    List<Watchlist> findByUserId(Long userId);

    boolean existsByUserIdAndVesselImo(Long userId, String vesselImo);

    void deleteByUserIdAndVesselImo(Long userId, String vesselImo);
}
