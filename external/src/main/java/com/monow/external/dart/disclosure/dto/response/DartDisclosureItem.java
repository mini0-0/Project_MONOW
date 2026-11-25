package com.monow.external.dart.disclosure.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DartDisclosureItem(
        @JsonProperty("rcept_no") String receiptNumber,
        @JsonProperty("corp_code") String corpCode,
        @JsonProperty("report_nm") String reportName,
        @JsonProperty("rcept_dt") String receiptDate,
        @JsonProperty("flr_nm") String filerName
) {
}
