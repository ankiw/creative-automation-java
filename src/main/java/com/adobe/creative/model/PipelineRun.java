package com.adobe.creative.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated result of a full pipeline execution across all
 * products × markets × aspect ratios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineRun {

    private String campaignId;
    private int totalAssets;
    private int passed;
    private int failed;
    private double durationSeconds;

    @Builder.Default
    private List<AssetResult> results = new ArrayList<>();

    public void printSummary() {
        System.out.println("\n" + "=".repeat(64));
        System.out.printf("  Campaign Run  : %s%n", campaignId);
        System.out.printf("  Assets        : %d%n", totalAssets);
        System.out.printf("  Compliance    : %d%n", passed);
        System.out.printf("  Compliance   : %d%n", failed);
        System.out.printf("  Duration      : %.1fs%n", durationSeconds);
        System.out.println("=".repeat(64));
        results.forEach(r -> {
            System.out.println(r.getCompliance().summary());
            System.out.printf("  → %s  (%dms)%n%n", r.getOutputPath(), r.getDurationMs());
        });
        System.out.println("=".repeat(64));
    }
}
