package com.anchoriq.api.infrastructure.persistence.watchlist;

import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaWatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserId(Long userId);

    boolean existsByUserIdAndVesselImo(Long userId, String vesselImo);

    void deleteByUserIdAndVesselImo(Long userId, String vesselImo);
}
