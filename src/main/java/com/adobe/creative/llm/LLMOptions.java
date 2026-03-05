package com.adobe.creative.llm;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration options for LLM text generation.
 *
 * <h3>Key Parameters</h3>
 * <ul>
 *   <li><b>temperature</b> (0.0–1.0) – controls randomness. Lower = more deterministic,
 *       higher = more creative. Use 0.3 for factual tasks, 0.7–0.9 for creative copy.</li>
 *   <li><b>maxTokens</b> – maximum length of response. ~4 chars per token on average.
 *       100 tokens ≈ 75 words ≈ short paragraph.</li>
 *   <li><b>model</b> – specific model to use (e.g., "gpt-4", "claude-sonnet-4-6").
 *       If null, uses provider defaults.</li>
 * </ul>
 *
 * <h3>Preset Examples</h3>
 * <pre>{@code
 * // Creative ad copy
 * LLMOptions.creative()
 *
 * // Factual analysis
 * LLMOptions.factual()
 *
 * // Short responses
 * LLMOptions.concise()
 * }</pre>
 */
@Data
@Builder
public class LLMOptions {

    /**
     * Temperature (0.0–1.0). Controls randomness.
     * - 0.0–0.3: Deterministic, factual (compliance, analysis)
     * - 0.4–0.6: Balanced (translations, summaries)
     * - 0.7–1.0: Creative (ad copy, brainstorming)
     */
    @Builder.Default
    private double temperature = 0.7;

    /**
     * Maximum tokens to generate (~4 chars per token).
     * - 50: Short sentence (e.g., taglines)
     * - 200: Paragraph (e.g., ad copy)
     * - 500: Detailed response (e.g., analysis)
     * - 2000: Long-form content
     */
    @Builder.Default
    private int maxTokens = 500;

    /**
     * Specific model to use (optional).
     * - OpenAI: "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"
     * - Anthropic: "claude-sonnet-4-6", "claude-opus-4-6", "claude-haiku-4-5"
     * If null, uses provider defaults.
     */
    private String model;

    /**
     * Top-p sampling (0.0–1.0). Alternative to temperature.
     * Considers tokens with cumulative probability up to p.
     * Not exposed by default; most users prefer temperature.
     */
    @Builder.Default
    private Double topP = null;

    /**
     * Default options: balanced temperature, moderate length.
     * Good for general-purpose tasks.
     */
    public static LLMOptions defaults() {
        return LLMOptions.builder()
                .temperature(0.7)
                .maxTokens(500)
                .build();
    }

    /**
     * Creative preset: high temperature, longer responses.
     * Use for ad copy, brainstorming, content generation.
     */
    public static LLMOptions creative() {
        return LLMOptions.builder()
                .temperature(0.85)
                .maxTokens(300)
                .build();
    }

    /**
     * Factual preset: low temperature, moderate length.
     * Use for analysis, compliance checks, data extraction.
     */
    public static LLMOptions factual() {
        return LLMOptions.builder()
                .temperature(0.2)
                .maxTokens(500)
                .build();
    }

    /**
     * Concise preset: low temperature, short responses.
     * Use for taglines, titles, short summaries.
     */
    public static LLMOptions concise() {
        return LLMOptions.builder()
                .temperature(0.5)
                .maxTokens(100)
                .build();
    }

    /**
     * Translation preset: balanced temperature, moderate length.
     * Use for content localization and translations.
     */
    public static LLMOptions translation() {
        return LLMOptions.builder()
                .temperature(0.4)
                .maxTokens(400)
                .build();
    }

    /**
     * JSON preset: very low temperature for structured output.
     * Use for API responses, structured data extraction.
     */
    public static LLMOptions json() {
        return LLMOptions.builder()
                .temperature(0.1)
                .maxTokens(800)
                .build();
    }
}
