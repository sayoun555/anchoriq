package com.anchoriq.core.domain.operation.bookmark.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "vessel_imo"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vessel_imo", nullable = false, length = 20)
    private String vesselImo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Watchlist(Long userId, String vesselImo) {
        this.userId = userId;
        this.vesselImo = vesselImo;
        this.createdAt = LocalDateTime.now();
    }

    public static Watchlist create(Long userId, String vesselImo) {
        return new Watchlist(userId, vesselImo);
    }
}
