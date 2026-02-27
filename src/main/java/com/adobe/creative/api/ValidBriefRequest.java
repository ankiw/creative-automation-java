package com.adobe.creative.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure either briefYaml or briefFilePath is provided.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BriefRequestValidator.class)
@Documented
public @interface ValidBriefRequest {
    String message() default "Either briefYaml or briefFilePath must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
