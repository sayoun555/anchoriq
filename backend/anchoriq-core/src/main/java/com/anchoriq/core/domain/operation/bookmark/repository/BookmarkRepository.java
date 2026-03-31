package com.anchoriq.core.domain.operation.bookmark.repository;

import com.anchoriq.core.domain.operation.bookmark.model.Bookmark;

import java.util.List;
import java.util.Optional;

/**
 * 즐겨찾기 Repository 인터페이스.
 */
public interface BookmarkRepository {

    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findById(Long id);

    List<Bookmark> findByUserId(Long userId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);

    void deleteById(Long id);
}
