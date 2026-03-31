package com.anchoriq.api.dto.response.bookmark;

import com.anchoriq.core.domain.operation.bookmark.model.Bookmark;
import lombok.Builder;
import lombok.Getter;

/**
 * 즐겨찾기 응답 DTO.
 */
@Getter
@Builder
public class BookmarkResponse {

    private final Long id;
    private final String targetType;
    private final String targetId;
    private final String memo;
    private final String createdAt;

    public static BookmarkResponse from(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .targetType(bookmark.getTargetType())
                .targetId(bookmark.getTargetId())
                .memo(bookmark.getMemo())
                .createdAt(bookmark.getCreatedAt() != null ? bookmark.getCreatedAt().toString() : null)
                .build();
    }
}
