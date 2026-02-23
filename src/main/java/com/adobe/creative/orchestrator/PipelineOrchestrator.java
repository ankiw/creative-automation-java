package com.adobe.creative.orchestrator;

import com.adobe.creative.compliance.ComplianceChecker;
import com.adobe.creative.config.PipelineConfig;
import com.adobe.creative.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Main pipeline orchestrator.
 *
 * <p>Ties together all pipeline steps for a full campaign run:
 * <pre>
 *   for each product:
 *     load or generate hero image
 *     for each market × aspect ratio:
 *       compose creative  (branded overlay, logo, message)
 *       run compliance check
 *       save to storage
 * </pre>
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>The orchestrator is intentionally sequential for the POC. In production
 *       each product × market × aspect-ratio triple would be a task submitted to
 *       an {@code ExecutorService} (or an Airflow / Argo DAG task).</li>
 *   <li>Hero image generation is done <em>once per product</em> at the outermost
 *       loop level, so a 2-product × 2-market × 3-ratio brief costs only 2 API
 *       calls instead of 12.</li>
 *   <li>All results (including compliance details) are written to a JSON run
 *       report for audit trails.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final ImageGenerator    imageGenerator;
    private final CreativeComposer  composer;
    private final ComplianceChecker complianceChecker;
    private final PipelineConfig    config;
    private final ObjectMapper      objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Executes the full pipeline for a campaign brief.
     *
     * @param brief the parsed campaign brief
     * @return a {@link PipelineRun} with all results and statistics
     */
    public PipelineRun run(CampaignBrief brief) {
        long startMs = System.currentTimeMillis();
        List<AssetResult> results = new ArrayList<>();

        log.info("Starting pipeline run: {} | {} products × {} markets × {} ratios = {} assets",
                brief.getCampaignId(),
                brief.getProducts().size(),
                brief.getMarkets().size(),
                brief.getAspectRatios().size(),
                brief.totalAssetCount());

        for (Product product : brief.getProducts()) {
            log.info("── Product: {} ──", product.getName());

            // Generate (or load) hero image once per product
            BufferedImage baseHero = loadOrGenerateHero(product, brief);

            for (Market market : brief.getMarkets()) {
                for (AspectRatio ar : brief.getAspectRatios()) {
                    AssetResult result = processOne(product, market, ar, baseHero, brief.getOutputDir());
                    results.add(result);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startMs;
        long passed  = results.stream().filter(r -> r.getCompliance().isPassed()).count();
        long failed  = results.size() - passed;

        PipelineRun run = PipelineRun.builder()
                .campaignId(brief.getCampaignId())
                .totalAssets(results.size())
                .passed((int) passed)
                .failed((int) failed)
                .durationSeconds(elapsed / 1000.0)
                .results(results)
                .build();

        saveRunReport(run, brief.getOutputDir());
        return run;
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Returns the hero image for a product.
     * Uses the existing file at {@code heroImagePath} if available;
     * otherwise calls {@link ImageGenerator} to produce one.
     */
    private BufferedImage loadOrGenerateHero(Product product, CampaignBrief brief) {
        if (product.getHeroImagePath() != null) {
            File f = new File(product.getHeroImagePath());
            if (f.exists()) {
                try {
                    log.info("  Using existing hero: {}", product.getHeroImagePath());
                    return ImageIO.read(f);
                } catch (IOException e) {
                    log.warn("  Could not read existing hero ({}), regenerating.", e.getMessage());
                }
            }
        }

        // Pick the largest aspect ratio as the base generation canvas
        AspectRatio largestAr = brief.getAspectRatios().stream()
                .max((a, b) -> Integer.compare(a.getWidth() * a.getHeight(),
                                               b.getWidth() * b.getHeight()))
                .orElse(brief.getAspectRatios().get(0));

        String prompt = buildPrompt(product);
        try {
            return imageGenerator.generate(prompt, product, largestAr);
        } catch (IOException e) {
            log.error("  Image generation failed for {}: {}", product.getName(), e.getMessage());
            // Fallback to mock so the pipeline doesn't halt
            return imageGenerator.generateMock(product, largestAr);
        }
    }

    /**
     * Processes a single product × market × aspect-ratio combination.
     */
    private AssetResult processOne(
            Product product, Market market, AspectRatio ar,
            BufferedImage hero, String outputDir) {

        long t0 = System.currentTimeMillis();
        log.debug("  [{}/{}/{}] composing …", product.getId(), market.getRegion(), ar.getName());

        // 1. Compose
        BufferedImage creative = composer.compose(hero, product, market, ar);

        // 2. Save
        String outputKey  = String.format("%s/%s/%s.png",
                product.getId(), market.getRegion(), ar.getName());
        String outputPath = saveImage(creative, outputDir, outputKey);

        // 3. Compliance
        ComplianceReport compliance = complianceChecker.check(
                outputPath, product, market, ar.getName());

        long durationMs = System.currentTimeMillis() - t0;
        log.info("  {} [{}/{}] → {} ({}ms)",
                compliance.isPassed() ? "✅" : "❌",
                product.getId(), ar.getName(), outputPath, durationMs);

        return AssetResult.builder()
                .productId(product.getId())
                .region(market.getRegion())
                .aspectRatio(ar.getName())
                .outputPath(outputPath)
                .compliance(compliance)
                .durationMs(durationMs)
                .generatedAt(Instant.now())
                .build();
    }

    /**
     * Writes a {@link BufferedImage} to disk under {@code outputDir/key}.
     * Creates parent directories as needed.
     */
    private String saveImage(BufferedImage img, String outputDir, String key) {
        Path path = Paths.get(outputDir, key);
        try {
            Files.createDirectories(path.getParent());
            ImageIO.write(img, "PNG", path.toFile());
        } catch (IOException e) {
            log.error("Failed to save image to {}: {}", path, e.getMessage());
        }
        return path.toString();
    }

    /** Writes the full run report as JSON for audit / reporting. */
    private void saveRunReport(PipelineRun run, String outputDir) {
        Path reportPath = Paths.get(outputDir, "run_report.json");
        try {
            Files.createDirectories(reportPath.getParent());
            objectMapper.writeValue(reportPath.toFile(), run);
            log.info("Run report → {}", reportPath);
        } catch (IOException e) {
            log.error("Could not save run report: {}", e.getMessage());
        }
    }

    /**
     * Builds an image generation prompt from product context.
     * When {@code llmProvider != mock}, this would be enriched by an LLM call.
     */
    private String buildPrompt(Product product) {
        return String.format(
            "A professional, high-quality advertisement photo for '%s', a %s product. " +
            "Tagline theme: '%s'. " +
            "Mood: vibrant, modern, aspirational. " +
            "Color palette inspired by %s and %s. " +
            "Clean complementary background. Photorealistic product shot. " +
            "No text overlays. Studio lighting. Ultra HD.",
            product.getName(),
            product.getCategory(),
            product.getTagline(),
            product.getBrandColors().getPrimary(),
            product.getBrandColors().getSecondary()
        );
    }
}
