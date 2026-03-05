package com.adobe.creative.llm;

import com.adobe.creative.config.PipelineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Provider-agnostic LLM service for text generation tasks.
 *
 * <p>Supports the following providers (set via {@code pipeline.llm-provider}):
 * <ul>
 *   <li>{@code mock} – returns predefined responses for testing (no API key needed)</li>
 *   <li>{@code openai} – uses GPT-4 via OpenAI API</li>
 *   <li>{@code anthropic} – uses Claude via Anthropic API</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li>Ad copy generation with market-specific messaging</li>
 *   <li>Image prompt enhancement for better AI-generated visuals</li>
 *   <li>Compliance analysis with contextual understanding</li>
 *   <li>Content translation and localization</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * String adCopy = llmService.generate(
 *     "Create a 100-character ad for Eco Glow Moisturizer targeting US millennials",
 *     LLMOptions.builder()
 *         .maxTokens(100)
 *         .temperature(0.7)
 *         .build()
 * );
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final PipelineConfig config;

    /**
     * Generates text using the configured LLM provider.
     *
     * @param prompt the instruction or question for the LLM
     * @return the generated text response
     * @throws IOException if API call fails
     */
    public String generate(String prompt) throws IOException {
        return generate(prompt, LLMOptions.defaults());
    }

    /**
     * Generates text with custom options (temperature, max tokens, etc.).
     *
     * @param prompt  the instruction or question for the LLM
     * @param options generation parameters (temperature, tokens, etc.)
     * @return the generated text response
     * @throws IOException if API call fails
     */
    public String generate(String prompt, LLMOptions options) throws IOException {
        log.debug("LLM request [{}] – {} chars, temp={}, max_tokens={}",
                config.getLlmProvider(), prompt.length(),
                options.getTemperature(), options.getMaxTokens());

        String response = switch (config.getLlmProvider()) {
            case "openai"    -> generateOpenAI(prompt, options);
            case "anthropic" -> generateAnthropic(prompt, options);
            default          -> generateMock(prompt, options);
        };

        log.debug("LLM response – {} chars", response.length());
        return response;
    }

    /**
     * Generates text with system instructions and user prompt (chat format).
     * Useful for structured tasks like JSON generation or role-based responses.
     *
     * @param systemPrompt instructions about the AI's role and behavior
     * @param userPrompt   the actual user request
     * @param options      generation parameters
     * @return the generated text response
     * @throws IOException if API call fails
     */
    public String generateWithSystem(String systemPrompt, String userPrompt, LLMOptions options)
            throws IOException {

        return switch (config.getLlmProvider()) {
            case "openai"    -> generateOpenAIChat(systemPrompt, userPrompt, options);
            case "anthropic" -> generateAnthropicChat(systemPrompt, userPrompt, options);
            default          -> generateMock(userPrompt, options);
        };
    }

    /**
     * Batch generation for multiple prompts (more efficient than sequential calls).
     *
     * @param prompts list of prompts to process
     * @param options generation parameters
     * @return list of responses in the same order
     * @throws IOException if API call fails
     */
    public List<String> generateBatch(List<String> prompts, LLMOptions options)
            throws IOException {

        log.info("Batch LLM request – {} prompts via [{}]",
                prompts.size(), config.getLlmProvider());

        // For now, process sequentially. Production would use parallel streams
        // or provider-specific batch APIs
        return prompts.stream()
                .map(prompt -> {
                    try {
                        return generate(prompt, options);
                    } catch (IOException e) {
                        log.error("Batch item failed: {}", e.getMessage());
                        return "[ERROR: " + e.getMessage() + "]";
                    }
                })
                .toList();
    }

    // ── Mock Provider ───────────────────────────────────────────────────────

    private String generateMock(String prompt, LLMOptions options) {
        // Intelligent mock responses based on prompt keywords
        // Check more specific patterns first before generic ones
        String lower = prompt.toLowerCase();

        // Check for variations first (before generic campaign message)
        if (lower.contains("variations") || lower.contains("different campaign messages")) {
            // Return numbered list format for variations
            return "1. Shop now and discover the difference! Limited time offer.\n"
                 + "2. Transform your routine with our premium products today.\n"
                 + "3. Join thousands of satisfied customers. Shop the collection now.";
        }
        if (lower.contains("ad copy") || lower.contains("campaign message")) {
            return "Shop now and discover the difference! Limited time offer.";
        }
        if (lower.contains("enhance") && lower.contains("prompt")) {
            return "Professional product photography, studio lighting, clean white background, "
                 + "commercial advertising style, high resolution, vibrant colors, centered composition";
        }
        if (lower.contains("compliance") || lower.contains("analyze")) {
            return "{\"passed\": true, \"issues\": [], \"confidence\": 0.95}";
        }
        if (lower.contains("translate")) {
            return "[Translated text would appear here]";
        }

        return "This is a mock response from the LLM service. "
             + "Configure pipeline.llm-provider=openai or anthropic for real generation.";
    }

    // ── OpenAI Provider ─────────────────────────────────────────────────────

    private String generateOpenAI(String prompt, LLMOptions options) throws IOException {
        String json = """
            {
              "model": "%s",
              "messages": [{"role": "user", "content": "%s"}],
              "temperature": %.2f,
              "max_tokens": %d
            }
            """.formatted(
                options.getModel() != null ? options.getModel() : "gpt-4",
                escapeJson(prompt),
                options.getTemperature(),
                options.getMaxTokens()
            );

        String response = httpPost(
            "https://api.openai.com/v1/chat/completions",
            json,
            "Authorization", "Bearer " + config.getOpenaiApiKey(),
            "Content-Type", "application/json"
        );

        return extractJsonField(response, "content");
    }

    private String generateOpenAIChat(String systemPrompt, String userPrompt, LLMOptions options)
            throws IOException {

        String json = """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
              ],
              "temperature": %.2f,
              "max_tokens": %d
            }
            """.formatted(
                options.getModel() != null ? options.getModel() : "gpt-4",
                escapeJson(systemPrompt),
                escapeJson(userPrompt),
                options.getTemperature(),
                options.getMaxTokens()
            );

        String response = httpPost(
            "https://api.openai.com/v1/chat/completions",
            json,
            "Authorization", "Bearer " + config.getOpenaiApiKey(),
            "Content-Type", "application/json"
        );

        return extractJsonField(response, "content");
    }

    // ── Anthropic Provider ──────────────────────────────────────────────────

    private String generateAnthropic(String prompt, LLMOptions options) throws IOException {
        String json = """
            {
              "model": "%s",
              "messages": [{"role": "user", "content": "%s"}],
              "temperature": %.2f,
              "max_tokens": %d
            }
            """.formatted(
                options.getModel() != null ? options.getModel() : "claude-sonnet-4-6",
                escapeJson(prompt),
                options.getTemperature(),
                options.getMaxTokens()
            );

        String response = httpPost(
            "https://api.anthropic.com/v1/messages",
            json,
            "x-api-key", config.getAnthropicApiKey(),
            "anthropic-version", "2023-06-01",
            "Content-Type", "application/json"
        );

        return extractJsonField(response, "text");
    }

    private String generateAnthropicChat(String systemPrompt, String userPrompt, LLMOptions options)
            throws IOException {

        String json = """
            {
              "model": "%s",
              "system": "%s",
              "messages": [{"role": "user", "content": "%s"}],
              "temperature": %.2f,
              "max_tokens": %d
            }
            """.formatted(
                options.getModel() != null ? options.getModel() : "claude-sonnet-4-6",
                escapeJson(systemPrompt),
                escapeJson(userPrompt),
                options.getTemperature(),
                options.getMaxTokens()
            );

        String response = httpPost(
            "https://api.anthropic.com/v1/messages",
            json,
            "x-api-key", config.getAnthropicApiKey(),
            "anthropic-version", "2023-06-01",
            "Content-Type", "application/json"
        );

        return extractJsonField(response, "text");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String httpPost(String url, String body, String... headers) throws IOException {
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(90_000);

        for (int i = 0; i < headers.length - 1; i += 2) {
            conn.setRequestProperty(headers[i], headers[i + 1]);
        }

        try (var os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int status = conn.getResponseCode();
        if (status >= 400) {
            var errorStream = conn.getErrorStream();
            String error = errorStream != null ? new String(errorStream.readAllBytes()) : "Unknown error";
            throw new IOException("LLM API error (HTTP " + status + "): " + error);
        }

        return new String(conn.getInputStream().readAllBytes());
    }

    private String extractJsonField(String json, String field) {
        // Handle nested content field in OpenAI's choices[0].message.content
        if (field.equals("content") && json.contains("\"choices\"")) {
            int contentIdx = json.indexOf("\"content\"", json.indexOf("\"choices\""));
            if (contentIdx != -1) {
                int start = json.indexOf("\"", contentIdx + 10) + 1;
                int end = json.indexOf("\"", start);
                return unescapeJson(json.substring(start, end));
            }
        }

        // Handle Anthropic's content[0].text structure
        if (field.equals("text") && json.contains("\"content\"")) {
            int textIdx = json.indexOf("\"text\"", json.indexOf("\"content\""));
            if (textIdx != -1) {
                int start = json.indexOf("\"", textIdx + 7) + 1;
                int end = json.indexOf("\"", start);
                return unescapeJson(json.substring(start, end));
            }
        }

        // Fallback to simple field extraction
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) {
            throw new RuntimeException("Field '" + field + "' not found in response: " + json.substring(0, Math.min(200, json.length())));
        }

        int colon = json.indexOf(':', idx) + 1;
        int start = json.indexOf('"', colon) + 1;
        int end = json.indexOf('"', start);
        return unescapeJson(json.substring(start, end));
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        return text.replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");
    }
}
