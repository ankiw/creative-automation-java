package com.adobe.creative.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code pipeline.*} block from application.yml into a typed bean.
 *
 * <p>All external service credentials are read from environment variables
 * (with empty-string defaults so the app starts in mock mode without any
 * configuration). In production these would be injected by Kubernetes secrets
 * or AWS Secrets Manager via environment variables.
 */
@Data
@Component
@ConfigurationProperties(prefix = "pipeline")
public class PipelineConfig {

    /** Image generation provider: mock | openai | stability | firefly */
    private String imageProvider = "mock";

    /** LLM provider for prompt enrichment: mock | openai | anthropic */
    private String llmProvider = "mock";

    /** Storage backend: local | s3 | azure */
    private String storageProvider = "local";

    /** Base directory for local output storage */
    private String outputDir = "outputs";

    // ── API Keys ────────────────────────────────────────────────────────────
    private String openaiApiKey      = "";
    private String anthropicApiKey   = "";
    private String stabilityApiKey   = "";
    private String fireflyClientId   = "";
    private String fireflyClientSecret = "";

    // ── AWS ─────────────────────────────────────────────────────────────────
    private String awsBucket = "";
    private String awsRegion = "us-east-1";
}
