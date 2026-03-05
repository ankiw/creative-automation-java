package com.adobe.creative;

import com.adobe.creative.config.PipelineConfig;
import com.adobe.creative.llm.LLMOptions;
import com.adobe.creative.llm.LLMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LLMService}.
 *
 * <p>These tests run in mock mode by default (no API keys required).
 * To test real providers, set environment variables:
 * - OPENAI_API_KEY for OpenAI tests
 * - ANTHROPIC_API_KEY for Anthropic tests
 *
 * <p>Run with: {@code mvn test -Dtest=LLMServiceTest}
 */
@SpringBootTest
class LLMServiceTest {

    @Autowired
    private LLMService llmService;

    @Autowired
    private PipelineConfig config;

    @BeforeEach
    void setUp() {
        // Ensure mock mode for tests (can be overridden via application-test.yml)
        if (config.getLlmProvider() == null || config.getLlmProvider().isEmpty()) {
            config.setLlmProvider("mock");
        }
    }

    @Test
    void testBasicGeneration() throws IOException {
        String prompt = "Create a short tagline for a moisturizer product";
        String response = llmService.generate(prompt);

        assertNotNull(response, "Response should not be null");
        assertFalse(response.isEmpty(), "Response should not be empty");
        assertTrue(response.length() > 10, "Response should be substantial");

        System.out.println("Generated: " + response);
    }

    @Test
    void testGenerationWithOptions() throws IOException {
        LLMOptions options = LLMOptions.builder()
                .temperature(0.7)
                .maxTokens(100)
                .build();

        String response = llmService.generate(
                "Write a one-sentence ad for eco-friendly skincare",
                options
        );

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void testCreativePreset() throws IOException {
        String response = llmService.generate(
                "Create an exciting ad headline",
                LLMOptions.creative()
        );

        assertNotNull(response);
        assertTrue(response.length() > 5);
    }

    @Test
    void testFactualPreset() throws IOException {
        String response = llmService.generate(
                "Analyze the compliance of this text: Shop now!",
                LLMOptions.factual()
        );

        assertNotNull(response);
    }

    @Test
    void testConcisePreset() throws IOException {
        String response = llmService.generate(
                "Generate a 5-word tagline",
                LLMOptions.concise()
        );

        assertNotNull(response);
        System.out.println("Concise response: " + response);
    }

    @Test
    void testSystemPrompt() throws IOException {
        String system = "You are a helpful assistant that responds in JSON format.";
        String user = "Create a product object with name and price fields";

        String response = llmService.generateWithSystem(system, user, LLMOptions.json());

        assertNotNull(response);
        System.out.println("Structured response: " + response);
    }

    @Test
    void testBatchGeneration() throws IOException {
        List<String> prompts = List.of(
                "Create a tagline for Product A",
                "Create a tagline for Product B",
                "Create a tagline for Product C"
        );

        List<String> responses = llmService.generateBatch(prompts, LLMOptions.concise());

        assertEquals(3, responses.size(), "Should get 3 responses");
        responses.forEach(r -> {
            assertNotNull(r);
            assertFalse(r.isEmpty());
            System.out.println("Batch result: " + r);
        });
    }

    @Test
    void testMockProviderAdCopyResponse() throws IOException {
        config.setLlmProvider("mock");

        String response = llmService.generate(
                "Create ad copy for our summer sale",
                LLMOptions.defaults()
        );

        assertTrue(response.contains("Shop now") || response.contains("mock"),
                "Mock should return sensible ad copy or mock message");
    }

    @Test
    void testMockProviderComplianceResponse() throws IOException {
        config.setLlmProvider("mock");

        String response = llmService.generate(
                "Analyze compliance for this text",
                LLMOptions.factual()
        );

        assertTrue(response.contains("passed") || response.contains("compliance"),
                "Mock should return compliance-related response");
    }

    @Test
    void testMockProviderPromptEnhancement() throws IOException {
        config.setLlmProvider("mock");

        String response = llmService.generate(
                "Enhance this image prompt: moisturizer product",
                LLMOptions.defaults()
        );

        assertTrue(response.length() > 20,
                "Mock should return detailed prompt enhancement");
        System.out.println("Enhanced prompt: " + response);
    }

    @Test
    void testDefaultOptions() {
        LLMOptions defaults = LLMOptions.defaults();

        assertEquals(0.7, defaults.getTemperature());
        assertEquals(500, defaults.getMaxTokens());
    }

    @Test
    void testCustomOptionsBuilder() {
        LLMOptions custom = LLMOptions.builder()
                .temperature(0.5)
                .maxTokens(200)
                .model("custom-model")
                .build();

        assertEquals(0.5, custom.getTemperature());
        assertEquals(200, custom.getMaxTokens());
        assertEquals("custom-model", custom.getModel());
    }

    /**
     * Integration test for OpenAI (requires API key).
     * Skipped by default - enable by setting OPENAI_API_KEY environment variable.
     */
    @Test
    void testOpenAIIntegration() throws IOException {
        if (config.getOpenaiApiKey() == null || config.getOpenaiApiKey().isEmpty()) {
            System.out.println("Skipping OpenAI test - no API key configured");
            return;
        }

        config.setLlmProvider("openai");

        String response = llmService.generate(
                "Write a 10-word product description for eco-friendly moisturizer",
                LLMOptions.concise()
        );

        assertNotNull(response);
        assertTrue(response.length() > 10);
        System.out.println("OpenAI response: " + response);
    }

    /**
     * Integration test for Anthropic (requires API key).
     * Skipped by default - enable by setting ANTHROPIC_API_KEY environment variable.
     */
    @Test
    void testAnthropicIntegration() throws IOException {
        if (config.getAnthropicApiKey() == null || config.getAnthropicApiKey().isEmpty()) {
            System.out.println("Skipping Anthropic test - no API key configured");
            return;
        }

        config.setLlmProvider("anthropic");

        String response = llmService.generate(
                "Write a 10-word product description for eco-friendly moisturizer",
                LLMOptions.concise()
        );

        assertNotNull(response);
        assertTrue(response.length() > 10);
        System.out.println("Anthropic response: " + response);
    }
}
