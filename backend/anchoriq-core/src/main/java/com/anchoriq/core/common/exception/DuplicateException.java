package com.anchoriq.core.common.exception;

public class DuplicateException extends DomainException {

    public DuplicateException(String message) {
        super("DUPLICATE", message);
    }
}
