package com.monow.external.navernews.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NaverNewsSearchResponse(
        String lastBuildDate,
        Integer total,
        Integer start,
        Integer display,
        List<Item> items
) {
    public record Item(
            String title,
            @JsonProperty("originallink") String originalLink,
            String link,
            String description,
            String pubDate
    ) {
    }
}
