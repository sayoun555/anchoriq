package com.anchoriq.api.application.bookmark;

import com.anchoriq.api.dto.request.bookmark.BookmarkRequest;
import com.anchoriq.api.dto.response.bookmark.BookmarkResponse;

import java.util.List;

/**
 * 즐겨찾기 Application Service 인터페이스.
 */
public interface BookmarkApplicationService {

    BookmarkResponse addBookmark(Long userId, BookmarkRequest request);

    List<BookmarkResponse> getBookmarks(Long userId);

    void deleteBookmark(Long userId, Long bookmarkId);
}
