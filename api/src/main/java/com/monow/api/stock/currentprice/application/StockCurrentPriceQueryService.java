package com.monow.api.stock.currentprice.application;

import com.monow.api.external.kis.application.KisAccessTokenProvider;
import com.monow.api.external.kis.client.KisCurrentPriceClient;
import com.monow.api.external.kis.dto.response.KisCurrentPriceResponse;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.application.StockMetadataCacheService;
import com.monow.api.stock.realtime.dto.StockMetadata;
import com.monow.api.stock.currentprice.dto.response.StockCurrentPriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockCurrentPriceQueryService {

    private final StockMetadataCacheService stockMetadataCacheService;

    private final StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisCurrentPriceClient kisCurrentPriceClient;

    private final Clock clock;

    // 단건 현재가 조회
    public StockCurrentPriceResponse getCurrentPrice(String stockCode) {
        StockMetadata metadata = stockMetadataCacheService.getMetadata(stockCode);

        StockCurrentPriceMarketSelection selection = stockCurrentPriceMarketSelector.select();
        CurrentPriceMarketType marketType = selection.marketType();

        String accessToken =  kisAccessTokenProvider.getAccessToken();

        KisCurrentPriceResponse kisResponse = kisCurrentPriceClient.fetchCurrentPrice(
                accessToken,
                stockCode,
                marketType
        );

        if (kisResponse == null || !"0".equals(kisResponse.rtCd())) {
            log.warn(
                    "KIS 현재가 조회 실패. stockCode={}, marketType={}",
                    stockCode,
                    marketType
            );
            throw new BusinessException(ErrorCode.KIS_CURRENT_PRICE_FETCH_FAILED);
        }


        KisCurrentPriceResponse.Output output = kisResponse.output();

        if (output == null) {
            throw  new BusinessException(ErrorCode.KIS_CURRENT_PRICE_INVALID_RESPONSE);
        }

        LocalDateTime updatedAt = LocalDateTime.now(clock);

        try {
            return new StockCurrentPriceResponse(
                    marketType,
                    selection.marketStatus(),
                    selection.realtime(),
                    metadata.stockCode(),
                    metadata.stockName(),

                    output.marketName(),
                    output.industryName(),

                    new BigDecimal(output.currentPrice()),
                    new BigDecimal(output.changePrice()),

                    output.changeSign(),

                    new BigDecimal(output.changeRate()),

                    Long.parseLong(output.tradeVolume()),

                    new BigDecimal(output.tradeAmount()),
                    new BigDecimal(output.openPrice()),
                    new BigDecimal(output.highPrice()),
                    new BigDecimal(output.lowPrice()),

                    updatedAt
            );
        } catch (NumberFormatException exception) {
            log.warn(
                    "KIS 현재가 숫자 변환 실패 - stockCode={}, marketType={}",
                    stockCode,
                    marketType,
                    exception
            );
            throw new BusinessException(
                    ErrorCode.KIS_CURRENT_PRICE_INVALID_RESPONSE
            );
        }
    }
}
