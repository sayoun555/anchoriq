package com.anchoriq.api.infrastructure.persistence.watchlist;

import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;
import com.anchoriq.core.domain.operation.bookmark.repository.WatchlistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WatchlistRepositoryImpl implements WatchlistRepository {

    private final JpaWatchlistRepository jpaRepository;

    @Override
    public Watchlist save(Watchlist watchlist) {
        return jpaRepository.save(watchlist);
    }

    @Override
    public List<Watchlist> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndVesselImo(Long userId, String vesselImo) {
        return jpaRepository.existsByUserIdAndVesselImo(userId, vesselImo);
    }

    @Override
    public void deleteByUserIdAndVesselImo(Long userId, String vesselImo) {
        jpaRepository.deleteByUserIdAndVesselImo(userId, vesselImo);
    }
}
