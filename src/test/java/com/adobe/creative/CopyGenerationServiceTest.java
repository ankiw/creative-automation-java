package com.adobe.creative;

import com.adobe.creative.llm.CopyGenerationService;
import com.adobe.creative.model.BrandColors;
import com.adobe.creative.model.Market;
import com.adobe.creative.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CopyGenerationService}.
 *
 * <p>Tests run in mock LLM mode by default. Set LLM provider to "openai" or "anthropic"
 * with appropriate API keys to test real generation.
 */
@SpringBootTest
class CopyGenerationServiceTest {

    @Autowired
    private CopyGenerationService copyService;

    private Product createTestProduct() {
        return Product.builder()
                .id("eco_moisturizer")
                .name("Eco Glow Moisturizer")
                .category("Skincare")
                .tagline("Radiance from nature")
                .brandColors(BrandColors.builder()
                        .primary("#2EC4B6")
                        .secondary("#FFFFFF")
                        .accent("#FF9F1C")
                        .build())
                .build();
    }

    private Market createTestMarket() {
        return Market.builder()
                .region("US")
                .locale("en_US")
                .campaignMessage("Shop now and save 20%!")
                .prohibitedWords(List.of("cure", "miracle", "guaranteed"))
                .build();
    }

    @Test
    void testGenerateCampaignMessage() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        String copy = copyService.generateCampaignMessage(product, market, "professional");

        assertNotNull(copy, "Generated copy should not be null");
        assertFalse(copy.isEmpty(), "Generated copy should not be empty");
        assertTrue(copy.length() <= 150, "Copy should be concise (<= 150 chars)");

        System.out.println("Generated campaign message: " + copy);
    }

    @Test
    void testGenerateVariations() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        List<String> variations = copyService.generateVariations(product, market, 3);

        assertNotNull(variations, "Variations should not be null");
        assertTrue(variations.size() >= 2, "Should generate at least 2 variations");

        System.out.println("Generated variations:");
        variations.forEach(v -> System.out.println("  - " + v));
    }

    @Test
    void testEnhanceImagePrompt() throws IOException {
        Product product = createTestProduct();

        String enhanced = copyService.enhanceImagePrompt(product, "Instagram");

        assertNotNull(enhanced);
        assertTrue(enhanced.length() > 50, "Enhanced prompt should be detailed");
        assertTrue(enhanced.toLowerCase().contains("moisturizer")
                || enhanced.toLowerCase().contains("skincare")
                || enhanced.length() > 100,
                "Enhanced prompt should be product-relevant or detailed");

        System.out.println("Enhanced image prompt: " + enhanced);
    }

    @Test
    void testLocalizeCopy() throws IOException {
        String originalCopy = "Shop now and save 20%!";
        Market sourceMarket = createTestMarket();
        Market targetMarket = Market.builder()
                .region("FR")
                .locale("fr_FR")
                .campaignMessage("")
                .prohibitedWords(List.of("garanti", "miracle"))
                .build();

        String localized = copyService.localizeCopy(originalCopy, sourceMarket, targetMarket);

        assertNotNull(localized);
        assertFalse(localized.isEmpty());

        System.out.println("Original: " + originalCopy);
        System.out.println("Localized (FR): " + localized);
    }

    @Test
    void testAnalyzeCompliance() throws IOException {
        Market market = createTestMarket();

        // Test with clean copy
        String cleanCopy = "Shop our new moisturizer today!";
        CopyGenerationService.ComplianceAnalysis analysis1 =
                copyService.analyzeCompliance(cleanCopy, market);

        assertNotNull(analysis1);
        System.out.println("Clean copy analysis: passed=" + analysis1.passed()
                + ", confidence=" + analysis1.confidence());

        // Test with problematic copy
        String problematicCopy = "This miracle cure is guaranteed to work!";
        CopyGenerationService.ComplianceAnalysis analysis2 =
                copyService.analyzeCompliance(problematicCopy, market);

        assertNotNull(analysis2);
        System.out.println("Problematic copy analysis: passed=" + analysis2.passed()
                + ", issues=" + analysis2.issues());
    }

    @Test
    void testProfessionalTone() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        String copy = copyService.generateCampaignMessage(product, market, "professional");

        assertNotNull(copy);
        System.out.println("Professional tone: " + copy);
    }

    @Test
    void testPlayfulTone() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        String copy = copyService.generateCampaignMessage(product, market, "playful");

        assertNotNull(copy);
        System.out.println("Playful tone: " + copy);
    }

    @Test
    void testUrgentTone() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        String copy = copyService.generateCampaignMessage(product, market, "urgent");

        assertNotNull(copy);
        System.out.println("Urgent tone: " + copy);
    }

    @Test
    void testProhibitedWordsRespected() throws IOException {
        Product product = createTestProduct();
        Market market = createTestMarket();

        String copy = copyService.generateCampaignMessage(product, market, "professional");

        // Should not contain prohibited words
        String lowerCopy = copy.toLowerCase();
        market.getProhibitedWords().forEach(word -> {
            assertFalse(lowerCopy.contains(word.toLowerCase()),
                    "Copy should not contain prohibited word: " + word);
        });
    }
}
