package com.adobe.creative.llm;

import com.adobe.creative.model.Market;
import com.adobe.creative.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * AI-powered ad copy generation service.
 *
 * <p>Generates market-specific, compliance-aware advertising copy using LLMs.
 * Automatically considers:
 * <ul>
 *   <li>Product details (name, category, tagline)</li>
 *   <li>Market regulations (prohibited words)</li>
 *   <li>Regional locale and cultural context</li>
 *   <li>Character limits for different platforms</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * String copy = copyService.generateCampaignMessage(product, market, "urgent");
 * // Output: "Last chance! Eco Glow Moisturizer - 20% off this weekend only"
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyGenerationService {

    private final LLMService llmService;

    /**
     * Generates a campaign message for a specific product and market.
     *
     * @param product the product to advertise
     * @param market  the target market with locale and restrictions
     * @param tone    desired tone: "professional", "playful", "urgent", "elegant"
     * @return generated ad copy (typically 80-120 characters)
     * @throws IOException if LLM API call fails
     */
    public String generateCampaignMessage(Product product, Market market, String tone)
            throws IOException {

        String systemPrompt = """
            You are an expert advertising copywriter specializing in social media ads.
            Create compelling, concise ad copy that drives action while respecting brand guidelines.
            Always stay within character limits and avoid prohibited language.
            """;

        String userPrompt = String.format("""
            Create a %s campaign message for:
            Product: %s (%s category)
            Tagline: "%s"
            Market: %s (%s)

            Requirements:
            - Maximum 120 characters
            - %s tone
            - Include a clear call-to-action
            - NEVER use these words: %s
            - Align with the product tagline

            Return only the ad copy, no explanations.
            """,
            tone,
            product.getName(),
            product.getCategory(),
            product.getTagline(),
            market.getRegion(),
            market.getLocale(),
            tone,
            market.getProhibitedWords().isEmpty() ? "none" : String.join(", ", market.getProhibitedWords())
        );

        String copy = llmService.generateWithSystem(
                systemPrompt,
                userPrompt,
                LLMOptions.creative()
        );

        // Clean up any quotes or extra whitespace
        copy = copy.trim().replaceAll("^\"|\"$", "");

        log.info("Generated copy for {} / {} [{}]: {}",
                product.getId(), market.getRegion(), tone, copy);

        return copy;
    }

    /**
     * Generates multiple copy variations for A/B testing.
     *
     * @param product      the product to advertise
     * @param market       the target market
     * @param numVariations number of different versions to generate (2-5 recommended)
     * @return list of copy variations
     * @throws IOException if LLM API call fails
     */
    public List<String> generateVariations(Product product, Market market, int numVariations)
            throws IOException {

        log.info("Generating {} copy variations for {} / {}",
                numVariations, product.getId(), market.getRegion());

        String systemPrompt = """
            You are an expert advertising copywriter creating A/B test variations.
            Generate diverse approaches that test different psychological triggers:
            urgency, exclusivity, benefit-focus, emotion, social proof.
            """;

        String userPrompt = String.format("""
            Create %d different campaign messages for:
            Product: %s (%s)
            Tagline: "%s"
            Market: %s

            Requirements:
            - Each variation should test a different angle
            - Maximum 120 characters each
            - Include call-to-action
            - Avoid: %s

            Format as numbered list:
            1. [copy]
            2. [copy]
            etc.
            """,
            numVariations,
            product.getName(),
            product.getCategory(),
            product.getTagline(),
            market.getRegion(),
            market.getProhibitedWords().isEmpty() ? "none" : String.join(", ", market.getProhibitedWords())
        );

        String response = llmService.generateWithSystem(
                systemPrompt,
                userPrompt,
                LLMOptions.builder()
                        .temperature(0.85)
                        .maxTokens(500)
                        .build()
        );

        // Parse numbered list format
        return response.lines()
                .filter(line -> line.matches("^\\d+\\..*"))
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", "").trim())
                .map(line -> line.replaceAll("^\"|\"$", ""))
                .toList();
    }

    /**
     * Enhances an image generation prompt with detailed creative direction.
     *
     * @param product the product to visualize
     * @param platform target platform (Instagram, Facebook, etc.)
     * @return enhanced prompt with lighting, composition, style details
     * @throws IOException if LLM API call fails
     */
    public String enhanceImagePrompt(Product product, String platform) throws IOException {
        String systemPrompt = """
            You are an expert AI image prompt engineer for advertising photography.
            Transform basic product descriptions into detailed, professional prompts that
            produce high-quality commercial imagery.
            """;

        String userPrompt = String.format("""
            Enhance this product description into a detailed image generation prompt:

            Product: %s
            Category: %s
            Tagline: "%s"
            Brand Colors: %s, %s
            Platform: %s

            Include:
            - Photography style and lighting
            - Composition and framing
            - Background and environment
            - Mood and atmosphere
            - Color palette usage

            Format as a single detailed prompt (no bullet points).
            """,
            product.getName(),
            product.getCategory(),
            product.getTagline(),
            product.getBrandColors().getPrimary(),
            product.getBrandColors().getAccent(),
            platform
        );

        String enhanced = llmService.generateWithSystem(
                systemPrompt,
                userPrompt,
                LLMOptions.builder()
                        .temperature(0.6)
                        .maxTokens(300)
                        .build()
        );

        log.info("Enhanced image prompt for {}: {} chars", product.getId(), enhanced.length());
        return enhanced.trim();
    }

    /**
     * Translates and culturally adapts copy for a different market.
     *
     * @param originalCopy the source copy to translate
     * @param sourceMarket the original market
     * @param targetMarket the destination market
     * @return localized copy maintaining tone and effectiveness
     * @throws IOException if LLM API call fails
     */
    public String localizeCopy(String originalCopy, Market sourceMarket, Market targetMarket)
            throws IOException {

        String systemPrompt = """
            You are an expert in advertising localization and cultural adaptation.
            Translate copy while maintaining marketing effectiveness, adjusting idioms,
            cultural references, and emotional triggers for the target culture.
            """;

        String userPrompt = String.format("""
            Translate and culturally adapt this ad copy:

            Original: "%s"
            From: %s (%s)
            To: %s (%s)

            Requirements:
            - Maintain the tone and urgency
            - Adapt idioms and cultural references
            - Keep under 120 characters
            - Avoid: %s

            Return only the localized copy.
            """,
            originalCopy,
            sourceMarket.getRegion(),
            sourceMarket.getLocale(),
            targetMarket.getRegion(),
            targetMarket.getLocale(),
            targetMarket.getProhibitedWords().isEmpty() ? "none" : String.join(", ", targetMarket.getProhibitedWords())
        );

        String localized = llmService.generateWithSystem(
                systemPrompt,
                userPrompt,
                LLMOptions.translation()
        );

        return localized.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * Analyzes copy for potential compliance issues beyond keyword matching.
     *
     * @param copy    the ad copy to analyze
     * @param market  the target market with regulations
     * @return analysis report with potential issues and confidence score
     * @throws IOException if LLM API call fails
     */
    public ComplianceAnalysis analyzeCompliance(String copy, Market market) throws IOException {
        String systemPrompt = String.format("""
            You are a regulatory compliance expert for advertising in %s.
            Analyze ad copy for:
            - Misleading claims or implications
            - Health/medical claims
            - Unsubstantiated guarantees
            - Age-inappropriate content
            - Cultural sensitivities

            Be thorough but not overly cautious. Real violations only.
            """, market.getRegion());

        String userPrompt = String.format("""
            Analyze this ad copy for compliance:
            "%s"

            Known prohibited words: %s
            Market: %s

            Return JSON format:
            {
              "passed": true/false,
              "confidence": 0.0-1.0,
              "issues": ["issue 1", "issue 2"],
              "suggestions": ["fix 1", "fix 2"]
            }
            """,
            copy,
            market.getProhibitedWords().isEmpty() ? "none" : String.join(", ", market.getProhibitedWords()),
            market.getRegion()
        );

        String response = llmService.generateWithSystem(
                systemPrompt,
                userPrompt,
                LLMOptions.json()
        );

        // Parse JSON response (simplified - production would use Jackson)
        boolean passed = response.contains("\"passed\": true");
        double confidence = parseConfidence(response);
        List<String> issues = parseJsonArray(response, "issues");
        List<String> suggestions = parseJsonArray(response, "suggestions");

        return new ComplianceAnalysis(passed, confidence, issues, suggestions);
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    private double parseConfidence(String json) {
        try {
            int idx = json.indexOf("\"confidence\":");
            if (idx == -1) return 0.0;
            int start = json.indexOf(":", idx) + 1;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<String> parseJsonArray(String json, String fieldName) {
        try {
            int idx = json.indexOf("\"" + fieldName + "\":");
            if (idx == -1) return List.of();
            int start = json.indexOf("[", idx) + 1;
            int end = json.indexOf("]", start);
            String arrayContent = json.substring(start, end);
            return List.of(arrayContent.split(","))
                    .stream()
                    .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                    .filter(s -> !s.isEmpty())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Result of AI-powered compliance analysis.
     */
    public record ComplianceAnalysis(
            boolean passed,
            double confidence,
            List<String> issues,
            List<String> suggestions
    ) {}
}
