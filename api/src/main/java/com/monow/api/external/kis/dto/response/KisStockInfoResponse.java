package com.monow.api.external.kis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KisStockInfoResponse(
        @JsonProperty("rt_cd")
        String rtCd,

        @JsonProperty("msg_cd")
        String msgCd,

        @JsonProperty("msg1")
        String message,

        @JsonProperty("output")
        Output output
) {
        public record Output(
                @JsonProperty("pdno")
                String productNumber,

                @JsonProperty("std_pdno")
                String standardProductNumber,

                @JsonProperty("shtn_pdno")
                String shortProductNumber,

                @JsonProperty("prdt_name")
                String productName,

                @JsonProperty("prdt_abrv_name")
                String productShortName,

                @JsonProperty("prdt_type_cd")
                String productTypeCode,

                @JsonProperty("prdt_clsf_cd")
                String productClassCode,

                @JsonProperty("prdt_clsf_name")
                String productClassName,

                @JsonProperty("ivst_prdt_type_cd")
                String investmentProductTypeCode,

                @JsonProperty("ivst_prdt_type_cd_name")
                String investmentProductTypeName
        ) {

        }

}
