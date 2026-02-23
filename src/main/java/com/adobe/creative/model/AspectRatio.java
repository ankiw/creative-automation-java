package com.adobe.creative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines an output canvas size and the social platform it targets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AspectRatio {

    @JsonProperty("name")
    private String name;        // e.g. "square", "story", "landscape"

    @JsonProperty("width")
    private int width;          // pixels

    @JsonProperty("height")
    private int height;         // pixels

    @JsonProperty("platform")
    private String platform;    // e.g. "Instagram Feed"

    /** Returns "1080x1920" style string – useful for logging and cache keys. */
    public String dimensions() {
        return width + "x" + height;
    }
}
