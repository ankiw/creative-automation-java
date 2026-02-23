package com.adobe.creative;

import com.adobe.creative.model.CampaignBrief;
import com.adobe.creative.parser.CampaignBriefParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CampaignBriefParser}.
 */
class CampaignBriefParserTest {

    private CampaignBriefParser parser;

    // Minimal valid YAML brief for testing
    private static final String VALID_YAML = """
        campaign_id: TEST_001
        campaign_name: Test Campaign
        target_audience: Millennials
        output_dir: outputs
        products:
          - id: product_a
            name: Product A
            category: Skincare
            tagline: Feel the glow
            brand_colors:
              primary: "#F4A261"
              secondary: "#FFFFFF"
              accent: "#264653"
          - id: product_b
            name: Product B
            category: Beverage
            tagline: Feel alive
            brand_colors:
              primary: "#2EC4B6"
              secondary: "#011627"
              accent: "#FF9F1C"
        markets:
          - region: US
            locale: en_US
            campaign_message: "Shop now."
            prohibited_words: [cure, miracle]
          - region: DE
            locale: de_DE
            campaign_message: "Jetzt shoppen."
        aspect_ratios:
          - { name: square,    width: 1080, height: 1080, platform: Instagram }
          - { name: story,     width: 1080, height: 1920, platform: TikTok }
          - { name: landscape, width: 1920, height: 1080, platform: Facebook }
        """;

    @BeforeEach
    void setUp() {
        parser = new CampaignBriefParser();
    }

    @Test
    @DisplayName("parseFromString deserializes a valid brief correctly")
    void parseFromString_validYaml_succeeds() throws IOException {
        CampaignBrief brief = parser.parseFromString(VALID_YAML);

        assertThat(brief.getCampaignId()).isEqualTo("TEST_001");
        assertThat(brief.getCampaignName()).isEqualTo("Test Campaign");
        assertThat(brief.getProducts()).hasSize(2);
        assertThat(brief.getMarkets()).hasSize(2);
        assertThat(brief.getAspectRatios()).hasSize(3);
    }

    @Test
    @DisplayName("Products are parsed with correct brand colors")
    void parseFromString_brandColors_correct() throws IOException {
        CampaignBrief brief = parser.parseFromString(VALID_YAML);

        var product = brief.getProducts().get(0);
        assertThat(product.getId()).isEqualTo("product_a");
        assertThat(product.getBrandColors().getPrimary()).isEqualTo("#F4A261");
        assertThat(product.getBrandColors().getSecondary()).isEqualTo("#FFFFFF");
        assertThat(product.getBrandColors().getAccent()).isEqualTo("#264653");
    }

    @Test
    @DisplayName("Markets are parsed with prohibited words list")
    void parseFromString_markets_prohibitedWords() throws IOException {
        CampaignBrief brief = parser.parseFromString(VALID_YAML);

        var usMarket = brief.getMarkets().get(0);
        assertThat(usMarket.getRegion()).isEqualTo("US");
        assertThat(usMarket.getCampaignMessage()).isEqualTo("Shop now.");
        assertThat(usMarket.getProhibitedWords()).containsExactly("cure", "miracle");
    }

    @Test
    @DisplayName("Aspect ratios have correct dimensions")
    void parseFromString_aspectRatios_dimensions() throws IOException {
        CampaignBrief brief = parser.parseFromString(VALID_YAML);

        var story = brief.getAspectRatios().get(1);
        assertThat(story.getName()).isEqualTo("story");
        assertThat(story.getWidth()).isEqualTo(1080);
        assertThat(story.getHeight()).isEqualTo(1920);
        assertThat(story.dimensions()).isEqualTo("1080x1920");
    }

    @Test
    @DisplayName("totalAssetCount() = products × markets × aspect ratios")
    void totalAssetCount_correctMultiplication() throws IOException {
        CampaignBrief brief = parser.parseFromString(VALID_YAML);
        // 2 products × 2 markets × 3 ratios = 12
        assertThat(brief.totalAssetCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("Validation fails if campaign_id is missing")
    void parseFromString_missingCampaignId_throws() {
        String yaml = VALID_YAML.replace("campaign_id: TEST_001", "campaign_id: ");
        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaign_id");
    }

    @Test
    @DisplayName("Validation fails if fewer than 3 aspect ratios provided")
    void parseFromString_twoAspectRatios_throws() {
        String yaml = VALID_YAML.replace(
            "  - { name: landscape, width: 1920, height: 1080, platform: Facebook }", "");
        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 aspect ratios");
    }

    @Test
    @DisplayName("Validation fails if a product has an invalid hex color")
    void parseFromString_invalidHexColor_throws() {
        String yaml = VALID_YAML.replace("\"#F4A261\"", "\"not-a-color\"");
        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hex color");
    }
}
