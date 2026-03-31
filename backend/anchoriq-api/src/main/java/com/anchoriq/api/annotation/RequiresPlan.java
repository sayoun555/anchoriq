package com.anchoriq.api.annotation;

import com.anchoriq.core.domain.account.subscription.model.Plan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPlan {
    Plan value();
}
