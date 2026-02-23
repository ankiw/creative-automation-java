package com.adobe.creative.parser;

import com.adobe.creative.model.CampaignBrief;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parses a campaign brief from YAML into a strongly-typed {@link CampaignBrief}.
 *
 * <p>Uses Jackson's YAML mapper, which delegates to SnakeYAML under the hood.
 * Supports loading from a file path, classpath resource, or raw InputStream.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li>Jackson YAML was chosen over Spring's {@code @ConfigurationProperties}
 *       because the brief is a runtime input, not a static app config.</li>
 *   <li>The mapper is created once and reused (thread-safe after configuration).</li>
 *   <li>All fields use {@code snake_case} JSON property names to match the YAML
 *       schema without requiring a naming strategy override.</li>
 * </ul>
 */
@Slf4j
@Component
public class CampaignBriefParser {

    private final ObjectMapper yamlMapper;

    public CampaignBriefParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        // Allow unknown fields so the brief can contain extra metadata
        // without breaking the parser
        this.yamlMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
    }

    /**
     * Load a brief from an absolute or relative file path.
     *
     * @param filePath path to the .yaml brief file
     * @return parsed {@link CampaignBrief}
     * @throws IOException if the file cannot be read or is malformed
     */
    public CampaignBrief parseFromFile(String filePath) throws IOException {
        log.info("Parsing campaign brief from file: {}", filePath);
        CampaignBrief brief = yamlMapper.readValue(new File(filePath), CampaignBrief.class);
        validate(brief);
        log.info("Parsed brief '{}': {} products, {} markets, {} aspect ratios",
                brief.getCampaignName(),
                brief.getProducts().size(),
                brief.getMarkets().size(),
                brief.getAspectRatios().size());
        return brief;
    }

    /**
     * Load a brief from a classpath resource (useful for testing).
     *
     * @param resourcePath e.g. "/briefs/test_campaign.yaml"
     */
    public CampaignBrief parseFromClasspath(String resourcePath) throws IOException {
        log.info("Parsing campaign brief from classpath: {}", resourcePath);
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Classpath resource not found: " + resourcePath);
            }
            CampaignBrief brief = yamlMapper.readValue(is, CampaignBrief.class);
            validate(brief);
            return brief;
        }
    }

    /**
     * Load a brief from a raw YAML string (useful for API endpoints).
     */
    public CampaignBrief parseFromString(String yamlContent) throws IOException {
        log.debug("Parsing campaign brief from inline YAML string");
        CampaignBrief brief = yamlMapper.readValue(yamlContent, CampaignBrief.class);
        validate(brief);
        return brief;
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Basic structural validation – throws {@link IllegalArgumentException}
     * if the brief is missing required fields.
     *
     * <p>In a production service this would use Bean Validation (@Valid) on
     * the model classes themselves, but keeping it explicit here makes the
     * rules easy to find and discuss.
     */
    private void validate(CampaignBrief brief) {
        if (brief.getCampaignId() == null || brief.getCampaignId().isBlank()) {
            throw new IllegalArgumentException("Brief is missing 'campaign_id'");
        }
        if (brief.getProducts() == null || brief.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Brief must contain at least one product");
        }
        if (brief.getMarkets() == null || brief.getMarkets().isEmpty()) {
            throw new IllegalArgumentException("Brief must contain at least one market");
        }
        if (brief.getAspectRatios() == null || brief.getAspectRatios().size() < 3) {
            throw new IllegalArgumentException("Brief must specify at least 3 aspect ratios");
        }

        // Validate each product has brand colors
        brief.getProducts().forEach(p -> {
            if (p.getBrandColors() == null) {
                throw new IllegalArgumentException(
                    "Product '" + p.getId() + "' is missing brand_colors");
            }
            validateHexColor(p.getBrandColors().getPrimary(),   p.getId() + ".brand_colors.primary");
            validateHexColor(p.getBrandColors().getSecondary(), p.getId() + ".brand_colors.secondary");
            validateHexColor(p.getBrandColors().getAccent(),    p.getId() + ".brand_colors.accent");
        });

        // Validate each market has a campaign message
        brief.getMarkets().forEach(m -> {
            if (m.getCampaignMessage() == null || m.getCampaignMessage().isBlank()) {
                throw new IllegalArgumentException(
                    "Market '" + m.getRegion() + "' is missing campaign_message");
            }
        });
    }

    private void validateHexColor(String hex, String field) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException(
                "Invalid hex color for field '" + field + "': " + hex);
        }
    }
}
