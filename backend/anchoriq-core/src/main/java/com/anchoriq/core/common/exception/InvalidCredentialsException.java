package com.anchoriq.core.common.exception;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password");
    }
}
