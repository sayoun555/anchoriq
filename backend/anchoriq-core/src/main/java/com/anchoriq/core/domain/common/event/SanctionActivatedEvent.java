package com.anchoriq.core.domain.common.event;

/**
 * 제재 활성화 도메인 이벤트.
 */
public class SanctionActivatedEvent extends DomainEvent {

    private final String referenceNumber;
    private final String targetName;
    private final String source;

    public SanctionActivatedEvent(String referenceNumber, String targetName, String source) {
        super();
        this.referenceNumber = referenceNumber;
        this.targetName = targetName;
        this.source = source;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return String.format("SanctionActivated{ref='%s', target='%s'}", referenceNumber, targetName);
    }
}
