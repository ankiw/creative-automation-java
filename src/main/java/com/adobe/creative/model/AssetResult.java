package com.adobe.creative.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of generating + composing a single creative asset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResult {

    private String productId;
    private String region;
    private String aspectRatio;
    private String outputPath;
    private ComplianceReport compliance;
    private long durationMs;
    private Instant generatedAt;
}


