package com.adobe.creative.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/pipeline/run.
 * The caller can either provide the YAML brief inline or reference a file path.
 */
@Data
public class RunPipelineRequest {

    /** Inline YAML brief (mutually exclusive with briefFilePath). */
    private String briefYaml;

    /** Path to a YAML brief file on the server (mutually exclusive with briefYaml). */
    private String briefFilePath;

    /** Override output directory (optional). */
    private String outputDir;
}
