package com.anchoriq.core.common.exception;

public class ConcurrentRequestException extends DomainException {

    public ConcurrentRequestException(String message) {
        super("CONCURRENT_REQUEST", message);
    }
}
