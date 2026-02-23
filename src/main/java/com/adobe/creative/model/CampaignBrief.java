package com.adobe.creative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Top-level campaign brief – the primary input to the pipeline.
 * Loaded from a YAML file via {@link com.adobe.creative.parser.CampaignBriefParser}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignBrief {

    @JsonProperty("campaign_id")
    private String campaignId;

    @JsonProperty("campaign_name")
    private String campaignName;

    @JsonProperty("target_audience")
    private String targetAudience;

    @JsonProperty("products")
    private List<Product> products;

    @JsonProperty("markets")
    private List<Market> markets;

    @JsonProperty("aspect_ratios")
    private List<AspectRatio> aspectRatios;

    @JsonProperty("output_dir")
    private String outputDir;

    /** Convenience: total asset count = products × markets × aspect ratios */
    public int totalAssetCount() {
        return products.size() * markets.size() * aspectRatios.size();
    }
}
