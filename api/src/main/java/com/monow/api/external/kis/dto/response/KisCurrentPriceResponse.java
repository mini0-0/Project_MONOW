package com.monow.api.external.kis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisCurrentPriceResponse(
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
                @JsonProperty("stck_shrn_iscd")
                String stockCode,

                // 대표 시장 한글명
                @JsonProperty("rprs_mrkt_kor_name")
                String marketName,

                // 업종 한글명
                @JsonProperty("bstp_kor_isnm")
                String industryName,

                // 현재 주식가
                @JsonProperty("stck_prpr")
                String currentPrice,

                // 전일 대비 가격
                @JsonProperty("prdy_vrss")
                String changePrice,

                // 전일 대비 부호
                @JsonProperty("prdy_vrss_sign")
                String changeSign,

                // 전일 대비율
                @JsonProperty("prdy_ctrt")
                String changeRate,

                // 누적 거래량
                @JsonProperty("acml_vol")
                String tradeVolume,

                // 누적 거래대금
                @JsonProperty("acml_tr_pbmn")
                String tradeAmount,

                // 시가
                @JsonProperty("stck_oprc")
                String openPrice,

                // 고가
                @JsonProperty("stck_hgpr")
                String highPrice,

                // 저가
                @JsonProperty("stck_lwpr")
                String lowPrice

        ) {
        }
}
