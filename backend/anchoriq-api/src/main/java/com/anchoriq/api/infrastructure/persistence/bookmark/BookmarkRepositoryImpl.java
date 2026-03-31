package com.anchoriq.api.infrastructure.persistence.bookmark;

import com.anchoriq.core.domain.operation.bookmark.model.Bookmark;
import com.anchoriq.core.domain.operation.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookmarkRepository 구현체.
 */
@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryImpl implements BookmarkRepository {

    private final JpaBookmarkRepository jpaBookmarkRepository;

    @Override
    public Bookmark save(Bookmark bookmark) {
        return jpaBookmarkRepository.save(bookmark);
    }

    @Override
    public Optional<Bookmark> findById(Long id) {
        return jpaBookmarkRepository.findById(id);
    }

    @Override
    public List<Bookmark> findByUserId(Long userId) {
        return jpaBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId) {
        return jpaBookmarkRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Override
    public void deleteById(Long id) {
        jpaBookmarkRepository.deleteById(id);
    }
}
