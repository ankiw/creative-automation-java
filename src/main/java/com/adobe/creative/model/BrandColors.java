package com.adobe.creative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds the three brand hex colors for a product.
 * Provides conversion to RGB tuples for image processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandColors {

    @JsonProperty("primary")
    private String primary;       // e.g. "#F4A261"

    @JsonProperty("secondary")
    private String secondary;     // e.g. "#FFFFFF"

    @JsonProperty("accent")
    private String accent;        // e.g. "#264653"

    /**
     * Converts a hex color string to an int[3] RGB array.
     * e.g. "#F4A261" → [244, 162, 97]
     */
    public static int[] hexToRgb(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
            Integer.parseInt(clean.substring(0, 2), 16),
            Integer.parseInt(clean.substring(2, 4), 16),
            Integer.parseInt(clean.substring(4, 6), 16)
        };
    }

    public int[] primaryRgb()   { return hexToRgb(primary);   }
    public int[] secondaryRgb() { return hexToRgb(secondary); }
    public int[] accentRgb()    { return hexToRgb(accent);    }
}
