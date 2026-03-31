package com.anchoriq.api.application.bookmark;

import com.anchoriq.api.dto.request.bookmark.BookmarkRequest;
import com.anchoriq.api.dto.response.bookmark.BookmarkResponse;
import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.operation.bookmark.model.Bookmark;
import com.anchoriq.core.domain.operation.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 즐겨찾기 Application Service 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkApplicationServiceImpl implements BookmarkApplicationService {

    private final BookmarkRepository bookmarkRepository;

    @Override
    @Transactional
    public BookmarkResponse addBookmark(Long userId, BookmarkRequest request) {
        if (bookmarkRepository.existsByUserIdAndTargetTypeAndTargetId(
                userId, request.targetType(), request.targetId())) {
            throw new DuplicateException("Bookmark already exists: " + request.targetType() + "/" + request.targetId());
        }

        Bookmark bookmark = Bookmark.create(userId, request.targetType(), request.targetId(), request.memo());
        Bookmark saved = bookmarkRepository.save(bookmark);
        log.info("Bookmark added: userId={}, type={}, targetId={}", userId, request.targetType(), request.targetId());
        return BookmarkResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId).stream()
                .map(BookmarkResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Bookmark", bookmarkId.toString()));
        if (!bookmark.isOwnedBy(userId)) {
            throw new EntityNotFoundException("Bookmark", bookmarkId.toString());
        }
        bookmarkRepository.deleteById(bookmarkId);
        log.info("Bookmark deleted: userId={}, bookmarkId={}", userId, bookmarkId);
    }
}
