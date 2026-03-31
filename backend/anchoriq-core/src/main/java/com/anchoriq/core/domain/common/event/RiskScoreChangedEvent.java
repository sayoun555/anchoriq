package com.anchoriq.core.domain.common.event;

/**
 * 리스크 점수 변경 도메인 이벤트.
 */
public class RiskScoreChangedEvent extends DomainEvent {

    private final String targetId;
    private final String targetType;
    private final int previousScore;
    private final int newScore;

    public RiskScoreChangedEvent(String targetId, String targetType, int previousScore, int newScore) {
        super();
        this.targetId = targetId;
        this.targetType = targetType;
        this.previousScore = previousScore;
        this.newScore = newScore;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public int getPreviousScore() {
        return previousScore;
    }

    public int getNewScore() {
        return newScore;
    }

    @Override
    public String toString() {
        return String.format("RiskScoreChanged{target='%s:%s', %d -> %d}", targetType, targetId, previousScore, newScore);
    }
}
