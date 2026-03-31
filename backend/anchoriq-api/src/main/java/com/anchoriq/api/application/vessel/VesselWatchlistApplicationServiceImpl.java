package com.anchoriq.api.application.vessel;

import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.core.domain.operation.bookmark.model.Watchlist;
import com.anchoriq.core.domain.operation.bookmark.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 관심 선박 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class VesselWatchlistApplicationServiceImpl implements VesselWatchlistApplicationService {

    private final WatchlistRepository watchlistRepository;
    private final VesselRepository vesselRepository;

    @Override
    @Transactional
    public void addToWatchlist(Long userId, String vesselImo) {
        if (!vesselRepository.existsByImo(vesselImo)) {
            throw new EntityNotFoundException("Vessel", vesselImo);
        }
        if (watchlistRepository.existsByUserIdAndVesselImo(userId, vesselImo)) {
            throw new DuplicateException("Vessel already in watchlist: " + vesselImo);
        }
        Watchlist watchlist = Watchlist.create(userId, vesselImo);
        watchlistRepository.save(watchlist);
    }

    @Override
    @Transactional
    public void removeFromWatchlist(Long userId, String vesselImo) {
        watchlistRepository.deleteByUserIdAndVesselImo(userId, vesselImo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Watchlist> getWatchlist(Long userId) {
        return watchlistRepository.findByUserId(userId);
    }
}
