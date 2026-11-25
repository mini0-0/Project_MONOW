package com.monow.external.dart.disclosure.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DartDisclosurePageResponse(
        String status,
        @JsonProperty("page_no") int pageNumber,
        @JsonProperty("total_page") int totalPages,
        @JsonProperty("list") List<DartDisclosureItem> items
) {
}
