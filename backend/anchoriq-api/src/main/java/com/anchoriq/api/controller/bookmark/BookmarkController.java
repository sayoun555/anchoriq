package com.anchoriq.api.controller.bookmark;

import com.anchoriq.api.application.bookmark.BookmarkApplicationService;
import com.anchoriq.api.dto.request.bookmark.BookmarkRequest;
import com.anchoriq.api.dto.response.bookmark.BookmarkResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 즐겨찾기 Controller.
 */
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkApplicationService bookmarkApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> addBookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookmarkRequest request) {
        BookmarkResponse response = bookmarkApplicationService.addBookmark(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<List<BookmarkResponse>> getBookmarks(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(bookmarkApplicationService.getBookmarks(principal.userId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        bookmarkApplicationService.deleteBookmark(principal.userId(), id);
        return ApiResponse.success();
    }
}
