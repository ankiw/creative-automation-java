package com.adobe.creative;

import com.adobe.creative.compliance.ComplianceChecker;
import com.adobe.creative.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ComplianceChecker}.
 * These tests run without Spring context (no @SpringBootTest) for fast feedback.
 */
class ComplianceCheckerTest {

    private ComplianceChecker checker;
    private Product sampleProduct;
    private Market  safeMarket;
    private Market  violatingMarket;

    @BeforeEach
    void setUp() {
        checker = new ComplianceChecker();

        sampleProduct = Product.builder()
                .id("hydra_glow_serum")
                .name("HydraGlow Serum")
                .category("Skincare")
                .tagline("Glow from within")
                .brandColors(BrandColors.builder()
                        .primary("#F4A261")
                        .secondary("#FFFFFF")
                        .accent("#264653")
                        .build())
                .build();

        safeMarket = Market.builder()
                .region("US")
                .locale("en_US")
                .campaignMessage("Your summer essential. Shop now.")
                .prohibitedWords(List.of("guaranteed", "cure", "miracle"))
                .build();

        violatingMarket = Market.builder()
                .region("US")
                .locale("en_US")
                .campaignMessage("Guaranteed to cure all problems instantly!")
                .prohibitedWords(List.of("guaranteed", "cure", "miracle"))
                .build();
    }

    // ── Legal text check ────────────────────────────────────────────────────

    @Test
    @DisplayName("scanProhibitedWords returns empty list when message is clean")
    void legalCheck_cleanMessage_returnsEmpty() {
        List<String> found = checker.scanProhibitedWords(
                "Your summer essential. Shop now.",
                List.of("guaranteed", "cure", "miracle"));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("scanProhibitedWords detects prohibited words case-insensitively")
    void legalCheck_violatingMessage_detectsWords() {
        List<String> found = checker.scanProhibitedWords(
                "Guaranteed to CURE all problems!",
                List.of("guaranteed", "cure", "miracle"));

        assertThat(found).containsExactlyInAnyOrder("guaranteed", "cure");
    }

    @Test
    @DisplayName("scanProhibitedWords handles null inputs gracefully")
    void legalCheck_nullInputs_returnsEmpty() {
        assertThat(checker.scanProhibitedWords(null, List.of("banned"))).isEmpty();
        assertThat(checker.scanProhibitedWords("some text", null)).isEmpty();
    }

    // ── Full compliance check (no image – text only) ────────────────────────

    @Test
    @DisplayName("check() passes when message contains no prohibited words")
    void fullCheck_cleanMarket_passes() {
        // Pass a non-existent path – the image check will warn but not error
        ComplianceReport report = checker.check(
                "non_existent.png", sampleProduct, safeMarket, "square");

        assertThat(report.isPassed()).isTrue();
        assertThat(report.getErrors()).isEmpty();
        assertThat(report.getProductId()).isEqualTo("hydra_glow_serum");
        assertThat(report.getRegion()).isEqualTo("US");
        assertThat(report.getAspectRatio()).isEqualTo("square");
    }

    @Test
    @DisplayName("check() fails when message contains prohibited words")
    void fullCheck_violatingMarket_fails() {
        ComplianceReport report = checker.check(
                "non_existent.png", sampleProduct, violatingMarket, "square");

        assertThat(report.isPassed()).isFalse();
        assertThat(report.getErrors()).hasSize(1);
        assertThat(report.getErrors().get(0)).contains("guaranteed").contains("cure");
    }

    @Test
    @DisplayName("ComplianceReport.summary() returns readable string")
    void reportSummary_format() {
        ComplianceReport report = ComplianceReport.builder()
                .productId("test_product")
                .region("US")
                .aspectRatio("square")
                .passed(false)
                .errors(List.of("Prohibited word found: miracle"))
                .warnings(List.of("Low brand color coverage"))
                .build();

        String summary = report.summary();
        assertThat(summary).contains("❌ FAIL");
        assertThat(summary).contains("test_product");
        assertThat(summary).contains("miracle");
        assertThat(summary).contains("Low brand color");
    }

    // ── BrandColors ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("BrandColors.hexToRgb converts hex correctly")
    void brandColors_hexToRgb_correct() {
        int[] rgb = BrandColors.hexToRgb("#F4A261");
        assertThat(rgb).containsExactly(244, 162, 97);
    }

    @Test
    @DisplayName("BrandColors.hexToRgb handles hex without leading #")
    void brandColors_hexToRgb_withoutHash() {
        int[] rgb = BrandColors.hexToRgb("264653");
        assertThat(rgb).containsExactly(38, 70, 83);
    }

    @Test
    @DisplayName("BrandColors primary/accent/secondary convenience methods delegate correctly")
    void brandColors_convenienceMethods() {
        BrandColors colors = BrandColors.builder()
                .primary("#FF0000")
                .secondary("#00FF00")
                .accent("#0000FF")
                .build();

        assertThat(colors.primaryRgb()).containsExactly(255, 0, 0);
        assertThat(colors.secondaryRgb()).containsExactly(0, 255, 0);
        assertThat(colors.accentRgb()).containsExactly(0, 0, 255);
    }
}
