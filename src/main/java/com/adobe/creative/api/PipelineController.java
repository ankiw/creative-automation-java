package com.adobe.creative.api;

import com.adobe.creative.model.CampaignBrief;
import com.adobe.creative.model.PipelineRun;
import com.adobe.creative.orchestrator.PipelineOrchestrator;
import com.adobe.creative.parser.CampaignBriefParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * REST API controller exposing the Creative Automation Pipeline as an HTTP microservice.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   GET  /api/pipeline/health       – health + config summary
 *   POST /api/pipeline/run          – run pipeline with inline YAML or file path
 *   POST /api/pipeline/upload       – upload a YAML brief file + run pipeline
 *   POST /api/pipeline/validate     – validate a brief without generating images
 * </pre>
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>The controller is kept thin – no business logic lives here.
 *       All pipeline logic is in {@link PipelineOrchestrator}.</li>
 *   <li>Errors return structured JSON (not stack traces) via
 *       {@link GlobalExceptionHandler}.</li>
 *   <li>In production this would be secured with OAuth2 / API key middleware
 *       and the {@code /run} endpoint would submit to an async job queue
 *       rather than blocking the HTTP thread.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
@Tag(name = "Creative Automation Pipeline", description = "AI-driven social media ad campaign generation")
public class PipelineController {

    private final PipelineOrchestrator orchestrator;
    private final CampaignBriefParser  parser;

    // ── GET /api/pipeline/health ──────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health and version info")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "creative-automation-pipeline",
            "version",   "1.0.0",
            "timestamp", Instant.now().toString()
        ));
    }

    // ── POST /api/pipeline/run ─────────────────────────────────────────────

    /**
     * Runs the full pipeline.
     *
     * <p>Accepts a JSON body with either:
     * <ul>
     *   <li>{@code briefYaml} – inline YAML string</li>
     *   <li>{@code briefFilePath} – server-side file path</li>
     * </ul>
     *
     * <p>Example cURL:
     * <pre>
     * curl -X POST http://localhost:8080/api/pipeline/run \
     *   -H "Content-Type: application/json" \
     *   -d '{"briefFilePath": "config/campaign_brief.yaml"}'
     * </pre>
     */
    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Run pipeline", description = "Execute full creative generation pipeline")
    @ApiResponse(responseCode = "200", description = "Pipeline completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<PipelineRun> run(@Valid @RequestBody RunPipelineRequest request) {
        log.info("POST /api/pipeline/run – briefFilePath={}, hasInlineYaml={}",
                request.getBriefFilePath(), request.getBriefYaml() != null);

        CampaignBrief brief = parseBrief(request);

        if (request.getOutputDir() != null) {
            brief.setOutputDir(request.getOutputDir());
        }

        PipelineRun result = orchestrator.run(brief);
        return ResponseEntity.ok(result);
    }

    // ── POST /api/pipeline/upload ──────────────────────────────────────────

    /**
     * Upload a YAML brief file as multipart/form-data and immediately run the pipeline.
     *
     * <p>Example cURL:
     * <pre>
     * curl -X POST http://localhost:8080/api/pipeline/upload \
     *   -F "brief=@config/campaign_brief.yaml"
     * </pre>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and run", description = "Upload YAML brief file and run pipeline")
    @ApiResponse(responseCode = "200", description = "Upload and execution successful")
    public ResponseEntity<PipelineRun> upload(@RequestPart("brief") MultipartFile file)
            throws IOException {

        log.info("POST /api/pipeline/upload – file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // Save to a temp file (parser needs a File object for stream position)
        Path tmp = Files.createTempFile("brief_", ".yaml");
        file.transferTo(tmp);

        try {
            CampaignBrief brief = parser.parseFromFile(tmp.toString());
            PipelineRun result  = orchestrator.run(brief);
            return ResponseEntity.ok(result);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ── POST /api/pipeline/validate ────────────────────────────────────────

    /**
     * Validates a campaign brief (parses + structural checks) without generating images.
     * Useful for a "pre-flight" check before committing to a full pipeline run.
     */
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate brief", description = "Validate campaign brief without generating images")
    @ApiResponse(responseCode = "200", description = "Brief is valid")
    @ApiResponse(responseCode = "400", description = "Invalid brief")
    public ResponseEntity<Map<String, Object>> validate(@Valid @RequestBody RunPipelineRequest request) {
        log.info("POST /api/pipeline/validate");
        CampaignBrief brief = parseBrief(request);

        return ResponseEntity.ok(Map.of(
            "valid",        true,
            "campaignId",   brief.getCampaignId(),
            "campaignName", brief.getCampaignName(),
            "products",     brief.getProducts().size(),
            "markets",      brief.getMarkets().size(),
            "aspectRatios", brief.getAspectRatios().size(),
            "totalAssets",  brief.totalAssetCount()
        ));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private CampaignBrief parseBrief(RunPipelineRequest request) {
        try {
            if (request.getBriefYaml() != null && !request.getBriefYaml().isBlank()) {
                return parser.parseFromString(request.getBriefYaml());
            }
            if (request.getBriefFilePath() != null && !request.getBriefFilePath().isBlank()) {
                return parser.parseFromFile(request.getBriefFilePath());
            }
            throw new IllegalArgumentException(
                "Request must include either 'briefYaml' or 'briefFilePath'");
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse campaign brief: " + e.getMessage(), e);
        }
    }
}
