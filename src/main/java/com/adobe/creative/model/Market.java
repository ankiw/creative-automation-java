package com.adobe.creative.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a target market/region for a campaign.
 * Each market gets its own localized campaign message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Market {

    @JsonProperty("region")
    private String region;              // e.g. "US", "DE"

    @JsonProperty("locale")
    private String locale;              // e.g. "en_US", "de_DE"

    @JsonProperty("campaign_message")
    private String campaignMessage;     // localized ad copy

    @JsonProperty("prohibited_words")
    @Builder.Default
    private List<String> prohibitedWords = new ArrayList<>();
}
