package com.adobe.creative.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of the compliance check for a single generated asset.
 * Captures both legal content violations (errors) and brand advisory issues (warnings).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReport {

    private String productId;
    private String region;
    private String aspectRatio;
    private boolean passed;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public String summary() {
        String status = passed ? "✅ PASS" : "❌ FAIL";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s | %s / %s / %s%n", status, productId, region, aspectRatio));
        warnings.forEach(w -> sb.append("  ⚠  ").append(w).append("\n"));
        errors.forEach(e   -> sb.append("  ✗  ").append(e).append("\n"));
        return sb.toString().trim();
    }
}
