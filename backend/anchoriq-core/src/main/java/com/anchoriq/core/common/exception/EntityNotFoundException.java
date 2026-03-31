package com.anchoriq.core.common.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, String identifier) {
        super("NOT_FOUND", String.format("%s not found with identifier: %s", entityName, identifier));
    }
}
