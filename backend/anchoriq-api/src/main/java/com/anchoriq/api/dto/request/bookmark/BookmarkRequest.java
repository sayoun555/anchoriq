package com.anchoriq.api.dto.request.bookmark;

import jakarta.validation.constraints.NotBlank;

/**
 * 즐겨찾기 추가 요청 DTO.
 */
public record BookmarkRequest(
        @NotBlank(message = "targetType is required") String targetType,
        @NotBlank(message = "targetId is required") String targetId,
        String memo
) {
}
