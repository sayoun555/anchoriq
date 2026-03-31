package com.anchoriq.api.infrastructure.persistence.bookmark;

import com.anchoriq.core.domain.operation.bookmark.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA Bookmark Repository.
 */
public interface JpaBookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);
}
