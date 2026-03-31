package com.anchoriq.core.domain.operation.collector.model;

/**
 * 수집기 마지막 실행 결과.
 */
public enum CollectorResult {

    SUCCESS,
    FAILED,
    NEVER_RUN;

    public boolean hasError() {
        return this == FAILED;
    }
}
