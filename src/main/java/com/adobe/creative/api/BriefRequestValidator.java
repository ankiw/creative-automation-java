package com.adobe.creative.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator that ensures either briefYaml or briefFilePath is provided in the request.
 */
public class BriefRequestValidator implements ConstraintValidator<ValidBriefRequest, RunPipelineRequest> {

    @Override
    public boolean isValid(RunPipelineRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        boolean hasYaml = request.getBriefYaml() != null && !request.getBriefYaml().isBlank();
        boolean hasFilePath = request.getBriefFilePath() != null && !request.getBriefFilePath().isBlank();

        return hasYaml || hasFilePath;
    }
}
