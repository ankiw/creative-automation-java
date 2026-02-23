package com.adobe.creative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single product in a campaign brief.
 * A campaign may include multiple products.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @JsonProperty("id")
    private String id;                  // e.g. "hydra_glow_serum"

    @JsonProperty("name")
    private String name;                // e.g. "HydraGlow Serum"

    @JsonProperty("category")
    private String category;            // e.g. "Skincare"

    @JsonProperty("tagline")
    private String tagline;             // e.g. "Glow from within"

    @JsonProperty("brand_colors")
    private BrandColors brandColors;

    @JsonProperty("logo_path")
    private String logoPath;            // optional – path to existing logo file

    @JsonProperty("hero_image_path")
    private String heroImagePath;       // null = generate via GenAI
}
