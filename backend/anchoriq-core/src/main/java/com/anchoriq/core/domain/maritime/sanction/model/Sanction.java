package com.anchoriq.core.domain.maritime.sanction.model;

import com.anchoriq.core.domain.common.event.SanctionActivatedEvent;
import com.anchoriq.core.domain.common.model.AggregateRoot;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 제재 엔티티 (Aggregate Root).
 * 제재의 활성 여부 및 대상 매칭 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Sanction extends AggregateRoot {

    private Long id;
    private String referenceNumber;
    private String targetName;
    private String type;
    private String source;
    private boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;

    protected Sanction() {
    }

    private Sanction(Builder builder) {
        this.referenceNumber = builder.referenceNumber;
        this.targetName = Objects.requireNonNull(builder.targetName, "Target name must not be null");
        this.type = Objects.requireNonNull(builder.type, "Type must not be null");
        this.source = Objects.requireNonNull(builder.source, "Source must not be null");
        this.active = true;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.description = builder.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Sanction create(String referenceNumber, String targetName, String type,
                                  String source, LocalDate startDate, LocalDate endDate,
                                  String description) {
        return builder()
                .referenceNumber(referenceNumber)
                .targetName(targetName)
                .type(type)
                .source(source)
                .startDate(startDate)
                .endDate(endDate)
                .description(description)
                .build();
    }

    // --- 비즈니스 로직 ---

    public boolean isCurrentlyActive() {
        if (!active) {
            return false;
        }
        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !now.isAfter(endDate);
    }

    public boolean matches(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return this.targetName.equalsIgnoreCase(name.trim());
    }

    public boolean matchesPartial(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return this.targetName.toLowerCase().contains(query.toLowerCase().trim());
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * 제재를 활성화하고 SanctionActivatedEvent를 발행한다.
     */
    public void activate() {
        this.active = true;
        registerEvent(new SanctionActivatedEvent(this.referenceNumber, this.targetName, this.source));
    }

    // --- 재구성용 ---

    public static Sanction reconstitute(Long id, String referenceNumber, String targetName,
                                         String type, String source, boolean active,
                                         LocalDate startDate, LocalDate endDate,
                                         String description) {
        Sanction sanction = new Sanction();
        sanction.id = id;
        sanction.referenceNumber = referenceNumber;
        sanction.targetName = targetName;
        sanction.type = type;
        sanction.source = source;
        sanction.active = active;
        sanction.startDate = startDate;
        sanction.endDate = endDate;
        sanction.description = description;
        return sanction;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sanction sanction = (Sanction) o;
        return Objects.equals(referenceNumber, sanction.referenceNumber)
                && targetName.equals(sanction.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceNumber, targetName);
    }

    @Override
    public String toString() {
        return String.format("Sanction{ref='%s', target='%s', active=%s}", referenceNumber, targetName, active);
    }

    // --- Builder ---

    public static class Builder {
        private String referenceNumber;
        private String targetName;
        private String type;
        private String source;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;

        private Builder() {
        }

        public Builder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public Builder targetName(String targetName) {
            this.targetName = targetName;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Sanction build() {
            return new Sanction(this);
        }
    }
}
