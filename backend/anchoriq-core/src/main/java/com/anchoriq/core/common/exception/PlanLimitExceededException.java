package com.anchoriq.core.common.exception;

public class PlanLimitExceededException extends DomainException {

    public PlanLimitExceededException(String message) {
        super("PLAN_LIMIT_EXCEEDED", message);
    }
}
