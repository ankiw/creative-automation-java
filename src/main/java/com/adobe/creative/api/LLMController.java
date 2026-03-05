package com.adobe.creative.api;

import com.adobe.creative.llm.CopyGenerationService;
import com.adobe.creative.llm.LLMOptions;
import com.adobe.creative.llm.LLMService;
import com.adobe.creative.model.Market;
import com.adobe.creative.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST API for AI-powered content generation.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Ad copy generation with market-specific messaging</li>
 *   <li>Image prompt enhancement for better AI-generated visuals</li>
 *   <li>Content translation and localization</li>
 *   <li>Compliance analysis with contextual understanding</li>
 *   <li>Creative variations for A/B testing</li>
 * </ul>
 *
 * <p>All endpoints work in mock mode by default (no API keys required).
 * Configure {@code pipeline.llm-provider} to use OpenAI or Anthropic.
 */
@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Tag(name = "LLM Services", description = "AI-powered content generation and analysis")
public class LLMController {

    private final LLMService llmService;
    private final CopyGenerationService copyService;

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify LLM service is available")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "llm-service",
                "capabilities", List.of(
                        "ad-copy-generation",
                        "prompt-enhancement",
                        "compliance-analysis",
                        "translation"
                )
        ));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate text", description = "Generate text using LLM with custom prompt")
    public ResponseEntity<GenerateResponse> generate(@RequestBody GenerateRequest request)
            throws IOException {

        LLMOptions options = LLMOptions.builder()
                .temperature(request.temperature != null ? request.temperature : 0.7)
                .maxTokens(request.maxTokens != null ? request.maxTokens : 500)
                .build();

        long startTime = System.currentTimeMillis();
        String response = llmService.generate(request.prompt, options);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(new GenerateResponse(
                response,
                response.length(),
                duration
        ));
    }

    @PostMapping("/copy/generate")
    @Operation(summary = "Generate campaign copy",
               description = "AI-generated ad copy for specific product and market")
    public ResponseEntity<CopyResponse> generateCopy(@RequestBody CopyRequest request)
            throws IOException {

        String copy = copyService.generateCampaignMessage(
                request.product,
                request.market,
                request.tone != null ? request.tone : "professional"
        );

        return ResponseEntity.ok(new CopyResponse(
                copy,
                copy.length(),
                request.tone != null ? request.tone : "professional"
        ));
    }

    @PostMapping("/copy/variations")
    @Operation(summary = "Generate copy variations",
               description = "Create multiple A/B test variations for ad copy")
    public ResponseEntity<VariationsResponse> generateVariations(
            @RequestBody VariationsRequest request) throws IOException {

        int numVariations = request.numVariations != null ? request.numVariations : 3;
        List<String> variations = copyService.generateVariations(
                request.product,
                request.market,
                numVariations
        );

        return ResponseEntity.ok(new VariationsResponse(
                variations,
                variations.size()
        ));
    }

    @PostMapping("/prompt/enhance")
    @Operation(summary = "Enhance image prompt",
               description = "Transform basic product description into detailed image generation prompt")
    public ResponseEntity<PromptResponse> enhancePrompt(@RequestBody PromptRequest request)
            throws IOException {

        String enhanced = copyService.enhanceImagePrompt(
                request.product,
                request.platform != null ? request.platform : "Instagram"
        );

        return ResponseEntity.ok(new PromptResponse(
                enhanced,
                enhanced.length()
        ));
    }

    @PostMapping("/compliance/analyze")
    @Operation(summary = "Analyze compliance",
               description = "AI-powered analysis of ad copy for regulatory compliance")
    public ResponseEntity<CopyGenerationService.ComplianceAnalysis> analyzeCompliance(
            @RequestBody ComplianceRequest request) throws IOException {

        return ResponseEntity.ok(
                copyService.analyzeCompliance(request.copy, request.market)
        );
    }

    @PostMapping("/translate")
    @Operation(summary = "Translate and localize",
               description = "Culturally adapt copy for different markets")
    public ResponseEntity<TranslationResponse> translate(@RequestBody TranslationRequest request)
            throws IOException {

        String localized = copyService.localizeCopy(
                request.copy,
                request.sourceMarket,
                request.targetMarket
        );

        return ResponseEntity.ok(new TranslationResponse(
                request.copy,
                localized,
                request.sourceMarket.getRegion(),
                request.targetMarket.getRegion()
        ));
    }

    // ── Request/Response DTOs ───────────────────────────────────────────────

    public record GenerateRequest(
            String prompt,
            Double temperature,
            Integer maxTokens
    ) {}

    public record GenerateResponse(
            String text,
            int characterCount,
            long durationMs
    ) {}

    public record CopyRequest(
            Product product,
            Market market,
            String tone
    ) {}

    public record CopyResponse(
            String copy,
            int characterCount,
            String tone
    ) {}

    public record VariationsRequest(
            Product product,
            Market market,
            Integer numVariations
    ) {}

    public record VariationsResponse(
            List<String> variations,
            int count
    ) {}

    public record PromptRequest(
            Product product,
            String platform
    ) {}

    public record PromptResponse(
            String enhancedPrompt,
            int characterCount
    ) {}

    public record ComplianceRequest(
            String copy,
            Market market
    ) {}

    public record TranslationRequest(
            String copy,
            Market sourceMarket,
            Market targetMarket
    ) {}

    public record TranslationResponse(
            String originalCopy,
            String localizedCopy,
            String sourceRegion,
            String targetRegion
    ) {}
}
