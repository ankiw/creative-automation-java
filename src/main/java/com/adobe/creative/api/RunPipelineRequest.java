package com.adobe.creative.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body for POST /api/pipeline/run.
 * The caller can either provide the YAML brief inline or reference a file path.
 */
@Data
@Schema(description = "Pipeline run request - provide either briefYaml or briefFilePath")
@ValidBriefRequest
public class RunPipelineRequest {

    @Schema(description = "Inline YAML campaign brief content", example = "campaign_id: DEMO_2026_Q1\ncampaign_name: Spring Launch\n...")
    private String briefYaml;

    @Schema(description = "Path to YAML brief file on server", example = "config/campaign_brief.yaml")
    private String briefFilePath;

    @Schema(description = "Override output directory (optional)", example = "outputs")
    private String outputDir;
}
