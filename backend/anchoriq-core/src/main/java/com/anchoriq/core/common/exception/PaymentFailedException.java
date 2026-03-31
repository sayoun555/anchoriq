package com.anchoriq.core.common.exception;

public class PaymentFailedException extends DomainException {

    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, cause);
    }
}
