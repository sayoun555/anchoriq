package com.anchoriq.core.domain.operation.bookmark.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 즐겨찾기 엔티티.
 * 항만, 항로, 초크포인트 등을 즐겨찾기할 수 있다.
 */
@Entity
@Table(name = "bookmarks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Bookmark(Long userId, String targetType, String targetId, String memo) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
    }

    public static Bookmark create(Long userId, String targetType, String targetId, String memo) {
        return new Bookmark(userId, targetType, targetId, memo);
    }

    public boolean isOwnedBy(Long requestUserId) {
        return this.userId.equals(requestUserId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return userId.equals(bookmark.userId)
                && targetType.equals(bookmark.targetType)
                && targetId.equals(bookmark.targetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, targetType, targetId);
    }
}
