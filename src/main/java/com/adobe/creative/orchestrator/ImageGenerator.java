package com.adobe.creative.orchestrator;

import com.adobe.creative.config.PipelineConfig;
import com.adobe.creative.model.AspectRatio;
import com.adobe.creative.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provider-agnostic image generation service.
 *
 * <p>Supports the following providers (set via {@code pipeline.image-provider}):
 * <ul>
 *   <li>{@code mock} – generates a branded gradient in pure Java AWT (no API key needed)</li>
 *   <li>{@code openai} – calls DALL-E 3 via OpenAI REST API</li>
 *   <li>{@code stability} – calls Stable Diffusion XL via Stability AI</li>
 *   <li>{@code firefly} – calls Adobe Firefly Services v3</li>
 * </ul>
 *
 * <h3>Caching</h3>
 * Generated images are cached to {@code .cache/images/} using an MD5 key of
 * {@code prompt + width + height}. Cache hits skip the API call entirely,
 * which is important when the same product is rendered for multiple markets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerator {

    private static final Path CACHE_DIR = Paths.get(".cache", "images");

    private final PipelineConfig config;

    /**
     * Returns a {@link BufferedImage} for the given prompt and dimensions.
     * Checks the disk cache first.
     */
    public BufferedImage generate(
            String prompt,
            Product product,
            AspectRatio aspectRatio) throws IOException {

        String cacheKey = buildCacheKey(prompt, aspectRatio.getWidth(), aspectRatio.getHeight());
        BufferedImage cached = loadFromCache(cacheKey);
        if (cached != null) {
            log.info("Cache HIT  – {} ({})", product.getName(), aspectRatio.dimensions());
            return cached;
        }

        log.info("Generating – {} ({}) via [{}] …",
                product.getName(), aspectRatio.dimensions(), config.getImageProvider());

        BufferedImage image = switch (config.getImageProvider()) {
            case "openai"    -> generateOpenAI(prompt, product, aspectRatio);
            case "stability" -> generateStability(prompt, product, aspectRatio);
            case "firefly"   -> generateFirefly(prompt, product, aspectRatio);
            default          -> generateMock(product, aspectRatio);
        };

        saveToCache(cacheKey, image);
        return image;
    }

    // ── Mock generator (Java AWT – no API key required) ────────────────────

    /**
     * Generates a branded gradient image using Java 2D.
     * Used as a fallback and for local development / CI.
     */
    public BufferedImage generateMock(Product product, AspectRatio aspectRatio) {
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ── Gradient background ─────────────────────────────────────────
        int[] c1 = product.getBrandColors().primaryRgb();
        int[] c2 = product.getBrandColors().accentRgb();
        Color top    = new Color(c1[0], c1[1], c1[2]);
        Color bottom = new Color(c2[0], c2[1], c2[2]);

        GradientPaint gradient = new GradientPaint(0, 0, top, 0, h, bottom);
        g.setPaint(gradient);
        g.fillRect(0, 0, w, h);

        // ── Decorative translucent circles (lifestyle aesthetic) ────────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(Color.WHITE);
        int seed = product.getId().hashCode();
        for (int i = 0; i < 7; i++) {
            seed = Math.abs(seed * 1664525 + 1013904223);  // LCG pseudo-random
            int cx = (Math.abs(seed >> 16) % w);
            int cy = (Math.abs(seed & 0xFFFF) % h);
            int r  = w / 10 + (Math.abs(seed >> 8) % (w / 4));
            g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            seed *= 6364136223846793005L;
        }

        // ── Product name (centered, large) ──────────────────────────────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        int titleSize = Math.max(w / 12, 32);
        int tagSize   = Math.max(w / 22, 18);

        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        drawCentered(g, product.getName(),
                new Font("SansSerif", Font.BOLD, titleSize), w, h / 2 + 2);
        // Text
        g.setColor(Color.WHITE);
        drawCentered(g, product.getName(),
                new Font("SansSerif", Font.BOLD, titleSize), w, h / 2);

        // Tagline
        g.setColor(new Color(255, 255, 255, 200));
        drawCentered(g, product.getTagline(),
                new Font("SansSerif", Font.PLAIN, tagSize), w, h / 2 + titleSize + 10);

        g.dispose();
        return img;
    }

    // ── OpenAI DALL-E 3 ─────────────────────────────────────────────────────

    private BufferedImage generateOpenAI(String prompt, Product product, AspectRatio ar)
            throws IOException {
        // DALL-E 3 supports 1024x1024, 1024x1792, 1792x1024
        String size = ar.getWidth() == ar.getHeight() ? "1024x1024"
                    : ar.getHeight() > ar.getWidth()  ? "1024x1792"
                    : "1792x1024";

        String json = """
            {"model":"dall-e-3","prompt":"%s","size":"%s","quality":"standard","n":1}
            """.formatted(prompt.replace("\"", "\\\""), size);

        String response = httpPost(
            "https://api.openai.com/v1/images/generations",
            json,
            "Authorization", "Bearer " + config.getOpenaiApiKey(),
            "Content-Type",  "application/json"
        );

        String url = extractJsonField(response, "url");
        BufferedImage img = ImageIO.read(new java.net.URL(url));
        return resizeTo(img, ar.getWidth(), ar.getHeight());
    }

    // ── Stability AI ─────────────────────────────────────────────────────────

    private BufferedImage generateStability(String prompt, Product product, AspectRatio ar)
            throws IOException {
        int w = snap64(ar.getWidth());
        int h = snap64(ar.getHeight());

        String json = """
            {"text_prompts":[{"text":"%s","weight":1.0}],"cfg_scale":7,"width":%d,"height":%d,"steps":30,"samples":1}
            """.formatted(prompt.replace("\"", "\\\""), w, h);

        String response = httpPost(
            "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
            json,
            "Authorization", "Bearer " + config.getStabilityApiKey(),
            "Accept",        "application/json",
            "Content-Type",  "application/json"
        );

        String b64 = extractJsonField(response, "base64");
        byte[] bytes = java.util.Base64.getDecoder().decode(b64);
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        return resizeTo(img, ar.getWidth(), ar.getHeight());
    }

    // ── Adobe Firefly Services ────────────────────────────────────────────────

    private BufferedImage generateFirefly(String prompt, Product product, AspectRatio ar)
            throws IOException {
        // Step 1: OAuth client_credentials token
        String tokenBody = "grant_type=client_credentials"
            + "&client_id=" + config.getFireflyClientId()
            + "&client_secret=" + config.getFireflyClientSecret()
            + "&scope=openid,AdobeID,firefly_api";

        String tokenResp = httpPost(
            "https://ims-na1.adobelogin.com/ims/token/v3",
            tokenBody,
            "Content-Type", "application/x-www-form-urlencoded"
        );
        String accessToken = extractJsonField(tokenResp, "access_token");

        // Step 2: Generate
        String json = """
            {"numVariations":1,"prompt":"%s","size":{"width":%d,"height":%d},"style":{"presets":["photo"]}}
            """.formatted(prompt.replace("\"", "\\\""), ar.getWidth(), ar.getHeight());

        String genResp = httpPost(
            "https://firefly-api.adobe.io/v3/images/generate",
            json,
            "Authorization", "Bearer " + accessToken,
            "X-Api-Key",     config.getFireflyClientId(),
            "Content-Type",  "application/json"
        );

        String url = extractJsonField(genResp, "url");
        BufferedImage img = ImageIO.read(new java.net.URL(url));
        return resizeTo(img, ar.getWidth(), ar.getHeight());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void drawCentered(Graphics2D g, String text, Font font, int canvasWidth, int y) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (canvasWidth - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private BufferedImage resizeTo(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private int snap64(int v) { return Math.max(64, (v / 64) * 64); }

    private String buildCacheKey(String prompt, int w, int h) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest((prompt + w + h).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return w + "x" + h + "_" + sb.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return w + "x" + h + "_" + Math.abs(prompt.hashCode());
        }
    }

    private BufferedImage loadFromCache(String key) {
        try {
            Path p = CACHE_DIR.resolve(key + ".png");
            if (Files.exists(p)) return ImageIO.read(p.toFile());
        } catch (IOException ignored) {}
        return null;
    }

    private void saveToCache(String key, BufferedImage img) {
        try {
            Files.createDirectories(CACHE_DIR);
            ImageIO.write(img, "PNG", CACHE_DIR.resolve(key + ".png").toFile());
        } catch (IOException e) {
            log.warn("Could not save to cache: {}", e.getMessage());
        }
    }

    /**
     * Minimal HTTP POST helper using only JDK classes (no additional deps).
     * In production you'd use WebClient (reactive) or RestTemplate.
     */
    private String httpPost(String url, String body, String... headers) throws IOException {
        java.net.HttpURLConnection conn =
            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(90_000);

        for (int i = 0; i < headers.length - 1; i += 2) {
            conn.setRequestProperty(headers[i], headers[i + 1]);
        }

        try (var os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int status = conn.getResponseCode();
        var stream = status < 400 ? conn.getInputStream() : conn.getErrorStream();
        return new String(stream.readAllBytes());
    }

    /** Very minimal JSON field extractor – avoids pulling in a JSON dep just for two fields. */
    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) throw new RuntimeException("Field '" + field + "' not found in: " + json);
        int colon  = json.indexOf(':', idx) + 1;
        char delim = json.charAt(skipWhitespace(json, colon));
        if (delim == '"') {
            int start = json.indexOf('"', colon) + 1;
            int end   = json.indexOf('"', start);
            return json.substring(start, end);
        }
        // number / bool
        int start = skipWhitespace(json, colon);
        int end   = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return json.substring(start, end).trim();
    }

    private int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }
}
