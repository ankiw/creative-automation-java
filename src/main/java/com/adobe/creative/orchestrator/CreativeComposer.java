package com.adobe.creative.orchestrator;

import com.adobe.creative.model.AspectRatio;
import com.adobe.creative.model.Market;
import com.adobe.creative.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Composes the final ad creative by layering over a hero image:
 * <ol>
 *   <li>Resize / smart-crop to target aspect ratio (cover strategy)</li>
 *   <li>Frosted-glass branded bar at bottom with campaign message + tagline</li>
 *   <li>Pill-shaped text logo in top-left corner</li>
 *   <li>Platform badge in bottom-right</li>
 * </ol>
 *
 * <p>All rendering is done with Java 2D ({@link Graphics2D}).
 * No external image libraries are required.
 */
@Slf4j
@Service
public class CreativeComposer {

    /**
     * Composes and returns the final {@link BufferedImage} for one asset.
     */
    public BufferedImage compose(
            BufferedImage heroImage,
            Product product,
            Market market,
            AspectRatio aspectRatio) {

        int W = aspectRatio.getWidth();
        int H = aspectRatio.getHeight();

        // 1. Smart crop hero to target canvas size
        BufferedImage canvas = coverResize(heroImage, W, H);

        // 2. Branded overlay bar (frosted glass effect)
        canvas = addBrandedBar(canvas, product, market, W, H);

        // 3. Text logo pill (top-left)
        canvas = stampTextLogo(canvas, product, W);

        // 4. Platform badge (bottom-right, above bar)
        canvas = stampPlatformBadge(canvas, aspectRatio.getPlatform(), W, H);

        return canvas;
    }

    // ── Step 1: Cover-resize ───────────────────────────────────────────────

    /**
     * Scales and center-crops {@code src} to fill {@code W×H} exactly.
     * This is the Java equivalent of CSS {@code background-size: cover}.
     */
    private BufferedImage coverResize(BufferedImage src, int W, int H) {
        double scaleX = (double) W / src.getWidth();
        double scaleY = (double) H / src.getHeight();
        double scale  = Math.max(scaleX, scaleY);

        int scaledW = (int) (src.getWidth()  * scale);
        int scaledH = (int) (src.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, scaledW, scaledH, null);
        g.dispose();

        // Center crop
        int cropX = (scaledW - W) / 2;
        int cropY = (scaledH - H) / 2;
        return scaled.getSubimage(cropX, cropY, W, H);
    }

    // ── Step 2: Branded overlay bar ────────────────────────────────────────

    private BufferedImage addBrandedBar(
            BufferedImage src, Product product, Market market, int W, int H) {

        int barH = Math.max(H / 6, 80);
        int barY = H - barH;

        BufferedImage out = deepCopy(src);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Gaussian blur on the strip behind the bar (frosted glass)
        BufferedImage strip = src.getSubimage(0, barY, W, barH);
        strip = applyBlur(strip, 10);
        g.drawImage(strip, 0, barY, null);

        // Colored semi-transparent overlay
        int[] rgb = product.getBrandColors().primaryRgb();
        g.setColor(new Color(rgb[0], rgb[1], rgb[2], 200));
        g.fillRect(0, barY, W, barH);

        // Campaign message (centered in bar, bold white)
        int msgSize = Math.max(W / 28, 22);
        int tagSize = Math.max(W / 40, 14);
        Font msgFont = new Font("SansSerif", Font.BOLD,  msgSize);
        Font tagFont = new Font("SansSerif", Font.PLAIN, tagSize);

        int msgY = barY + barH / 3;
        int tagY = barY + barH * 2 / 3;

        drawTextCentered(g, market.getCampaignMessage(), msgFont, W, msgY, Color.WHITE);

        // Accent-colored tagline
        int[] acc = product.getBrandColors().accentRgb();
        drawTextCentered(g, product.getTagline().toUpperCase(), tagFont, W, tagY,
                         new Color(acc[0], acc[1], acc[2]));

        g.dispose();
        return out;
    }

    // ── Step 3: Text logo pill ─────────────────────────────────────────────

    private BufferedImage stampTextLogo(BufferedImage src, Product product, int W) {
        int pad      = W / 40;
        int fontSize = Math.max(W / 30, 18);
        Font font    = new Font("SansSerif", Font.BOLD, fontSize);

        BufferedImage out = deepCopy(src);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(product.getName());
        int th = fm.getHeight();
        int pp = fontSize / 3;  // pill padding

        int pillW = tw + pp * 2;
        int pillH = th + pp * 2;
        int pillR = pillH / 2;   // corner radius = full pill

        // Pill background – use secondary brand color
        int[] sec = product.getBrandColors().secondaryRgb();
        g.setColor(new Color(sec[0], sec[1], sec[2], 230));
        g.fill(new RoundRectangle2D.Float(pad, pad, pillW, pillH, pillR, pillR));

        // Text – use primary brand color
        int[] pri = product.getBrandColors().primaryRgb();
        g.setColor(new Color(pri[0], pri[1], pri[2]));
        g.drawString(product.getName(), pad + pp, pad + pp + fm.getAscent());

        g.dispose();
        return out;
    }

    // ── Step 4: Platform badge ─────────────────────────────────────────────

    private BufferedImage stampPlatformBadge(
            BufferedImage src, String platform, int W, int H) {

        int barH     = Math.max(H / 6, 80);
        int fontSize = Math.max(W / 55, 12);
        Font font    = new Font("SansSerif", Font.PLAIN, fontSize);
        String label = "📱 " + platform;

        BufferedImage out = deepCopy(src);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int tw  = fm.stringWidth(label);
        int th  = fm.getHeight();
        int pad = W / 50;
        int x   = W - tw - pad;
        int y   = H - barH - th - pad;

        // Dark pill behind text
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Float(x - 4, y - 2, tw + 8, th + 4, 6, 6));
        g.setColor(Color.WHITE);
        g.drawString(label, x, y + fm.getAscent());

        g.dispose();
        return out;
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private void drawTextCentered(
            Graphics2D g, String text, Font font, int W, int y, Color color) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (W - fm.stringWidth(text)) / 2;
        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(text, x + 1, y + 1);
        // Text
        g.setColor(color);
        g.drawString(text, x, y);
    }

    /** Box-blur approximation of Gaussian blur using ConvolveOp. */
    private BufferedImage applyBlur(BufferedImage src, int radius) {
        int size   = radius * 2 + 1;
        float[] data = new float[size * size];
        float norm = 1.0f / (size * size);
        for (int i = 0; i < data.length; i++) data[i] = norm;
        Kernel kernel  = new Kernel(size, size, data);
        ConvolveOp op  = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage out = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        op.filter(src, out);
        return out;
    }

    /** Returns a deep copy so we never mutate the source image. */
    private BufferedImage deepCopy(BufferedImage src) {
        BufferedImage out = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
