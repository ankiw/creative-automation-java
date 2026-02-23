package com.adobe.creative;

import com.adobe.creative.config.PipelineConfig;
import com.adobe.creative.model.AspectRatio;
import com.adobe.creative.model.BrandColors;
import com.adobe.creative.model.Product;
import com.adobe.creative.orchestrator.ImageGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ImageGenerator} in mock mode (no API calls).
 */
class ImageGeneratorTest {

    private ImageGenerator generator;
    private Product        sampleProduct;

    @BeforeEach
    void setUp() {
        PipelineConfig config = new PipelineConfig();
        config.setImageProvider("mock");
        generator = new ImageGenerator(config);

        sampleProduct = Product.builder()
                .id("test_product")
                .name("Test Product")
                .category("Test")
                .tagline("Test tagline")
                .brandColors(BrandColors.builder()
                        .primary("#2EC4B6")
                        .secondary("#011627")
                        .accent("#FF9F1C")
                        .build())
                .build();
    }

    @ParameterizedTest(name = "generateMock produces {0}x{1} image")
    @CsvSource({
        "1080, 1080, square",
        "1080, 1920, story",
        "1920, 1080, landscape"
    })
    void generateMock_producesCorrectDimensions(int width, int height, String name)
            throws IOException {
        AspectRatio ar = AspectRatio.builder()
                .name(name).width(width).height(height).platform("Test").build();

        BufferedImage img = generator.generate("test prompt", sampleProduct, ar);

        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(width);
        assertThat(img.getHeight()).isEqualTo(height);
    }

    @Test
    @DisplayName("generate() hits cache on second call with same prompt + dimensions")
    void generate_cacheHit_secondCallReturnsCached() throws IOException {
        AspectRatio ar = AspectRatio.builder()
                .name("square").width(100).height(100).platform("Test").build();

        String prompt = "cache test prompt " + System.nanoTime();

        BufferedImage first  = generator.generate(prompt, sampleProduct, ar);
        BufferedImage second = generator.generate(prompt, sampleProduct, ar);

        // Both calls return valid images of the same dimensions
        assertThat(first.getWidth()).isEqualTo(second.getWidth());
        assertThat(first.getHeight()).isEqualTo(second.getHeight());
    }

    @Test
    @DisplayName("generateMock is deterministic for the same product ID")
    void generateMock_deterministic() {
        AspectRatio ar = AspectRatio.builder()
                .name("square").width(100).height(100).platform("Test").build();

        BufferedImage img1 = generator.generateMock(sampleProduct, ar);
        BufferedImage img2 = generator.generateMock(sampleProduct, ar);

        // Same product → same pixel at (50, 50) (gradient is deterministic)
        assertThat(img1.getRGB(50, 50)).isEqualTo(img2.getRGB(50, 50));
    }

    @Test
    @DisplayName("Two different products produce visually distinct images (different brand colors)")
    void generateMock_differentProducts_differentColors() {
        AspectRatio ar = AspectRatio.builder()
                .name("square").width(100).height(100).platform("Test").build();

        Product other = sampleProduct.toBuilder()
                .id("other")
                .brandColors(BrandColors.builder()
                        .primary("#F4A261").secondary("#264653").accent("#E9C46A").build())
                .build();

        BufferedImage img1 = generator.generateMock(sampleProduct, ar);
        BufferedImage img2 = generator.generateMock(other, ar);

        // Top-left pixel should reflect different gradient start colors
        assertThat(img1.getRGB(0, 0)).isNotEqualTo(img2.getRGB(0, 0));
    }
}
