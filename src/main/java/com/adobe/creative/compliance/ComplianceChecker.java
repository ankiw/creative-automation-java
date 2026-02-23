package com.adobe.creative.compliance;

import com.adobe.creative.model.ComplianceReport;
import com.adobe.creative.model.Market;
import com.adobe.creative.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compliance service that runs two checks on every generated asset:
 *
 * <ol>
 *   <li><b>Legal text check</b> – scans the campaign message for prohibited
 *       words defined per market (e.g. "guaranteed", "cure", "miracle").</li>
 *   <li><b>Brand color check</b> – samples the generated image and verifies
 *       that the primary brand color has sufficient pixel coverage (&ge;3%).</li>
 * </ol>
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>Errors (legal violations) block delivery; warnings (brand color) are
 *       advisory only. This maps to a real-world governance model.</li>
 *   <li>{@code BufferedImage} pixel sampling uses a configurable tolerance
 *       (Euclidean RGB distance) to account for JPEG compression artifacts
 *       and gradient blending in AI-generated images.</li>
 *   <li>The class is a Spring {@code @Service} so it can be injected and
 *       also mocked in unit tests with {@code @MockBean}.</li>
 * </ul>
 */
@Slf4j
@Service
public class ComplianceChecker {

    // Euclidean RGB distance tolerance (0–255 per channel)
    private static final int COLOR_TOLERANCE = 60;

    // Minimum fraction of pixels that must match the primary brand color
    private static final double MIN_COLOR_COVERAGE = 0.03;

    /**
     * Runs all compliance checks and returns a {@link ComplianceReport}.
     *
     * @param imagePath      path to the generated PNG on disk
     * @param product        the product whose brand rules to enforce
     * @param market         the market whose legal rules to enforce
     * @param aspectRatioName e.g. "square", "story", "landscape"
     */
    public ComplianceReport check(
            String imagePath,
            Product product,
            Market market,
            String aspectRatioName) {

        List<String> warnings = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        // ── 1. Legal text check ──────────────────────────────────────────
        List<String> flagged = scanProhibitedWords(
                market.getCampaignMessage(),
                market.getProhibitedWords());
        if (!flagged.isEmpty()) {
            errors.add("Prohibited word(s) found in campaign message: " + flagged);
            log.warn("[Compliance] LEGAL FAIL – {} / {} – prohibited: {}",
                    product.getId(), market.getRegion(), flagged);
        }

        // ── 2. Brand color check ─────────────────────────────────────────
        try {
            boolean hasColor = checkBrandColor(imagePath, product.getBrandColors().primaryRgb());
            if (!hasColor) {
                warnings.add(String.format(
                    "Primary brand color %s has low coverage (<%.0f%% of pixels). " +
                    "Consider adjusting the color scheme.",
                    product.getBrandColors().getPrimary(),
                    MIN_COLOR_COVERAGE * 100));
                log.warn("[Compliance] BRAND WARN – {} / {} – low primary color coverage",
                        product.getId(), market.getRegion());
            }
        } catch (IOException e) {
            warnings.add("Could not load image for color check: " + e.getMessage());
            log.error("[Compliance] Could not read image at {}: {}", imagePath, e.getMessage());
        }

        boolean passed = errors.isEmpty();

        return ComplianceReport.builder()
                .productId(product.getId())
                .region(market.getRegion())
                .aspectRatio(aspectRatioName)
                .passed(passed)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    // ── Legal text check ────────────────────────────────────────────────────

    /**
     * Returns the list of prohibited words found (case-insensitive) in {@code text}.
     */
    public List<String> scanProhibitedWords(String text, List<String> prohibitedWords) {
        if (text == null || prohibitedWords == null) return List.of();
        String lower = text.toLowerCase(Locale.ROOT);
        return prohibitedWords.stream()
                .filter(word -> lower.contains(word.toLowerCase(Locale.ROOT)))
                .toList();  // Java 16+ Stream.toList()
    }

    // ── Brand color check ───────────────────────────────────────────────────

    /**
     * Returns true if at least {@link #MIN_COLOR_COVERAGE} of the image pixels
     * are within {@link #COLOR_TOLERANCE} Euclidean distance from {@code targetRgb}.
     *
     * <p>Sampling every pixel of a 1920×1080 image (2M pixels) is fast enough
     * for a local pipeline. In production with high throughput you would sample
     * a random 10% subset or use a thumbnail.
     */
    public boolean checkBrandColor(String imagePath, int[] targetRgb) throws IOException {
        File file = new File(imagePath);
        if (!file.exists()) throw new IOException("Image not found: " + imagePath);

        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("Could not decode image: " + imagePath);

        int width  = img.getWidth();
        int height = img.getHeight();
        long total    = (long) width * height;
        long matching = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;

                double distance = Math.sqrt(
                    Math.pow(r - targetRgb[0], 2) +
                    Math.pow(g - targetRgb[1], 2) +
                    Math.pow(b - targetRgb[2], 2)
                );
                if (distance <= COLOR_TOLERANCE) matching++;
            }
        }

        double coverage = (double) matching / total;
        log.debug("[Compliance] Brand color coverage for {}: {:.1f}%",
                imagePath, coverage * 100);
        return coverage >= MIN_COLOR_COVERAGE;
    }
}
