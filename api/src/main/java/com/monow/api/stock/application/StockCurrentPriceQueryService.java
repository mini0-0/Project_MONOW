package com.monow.api.stock.application;

import com.monow.api.external.kis.application.KisAccessTokenProvider;
import com.monow.api.external.kis.client.KisCurrentPriceClient;
import com.monow.api.external.kis.dto.response.KisCurrentPriceResponse;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.StockMetadata;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockCurrentPriceQueryService {

    private final StockMetadataCacheService stockMetadataCacheService;

    private final StockRepository stockRepository;

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisCurrentPriceClient kisCurrentPriceClient;

    private final Clock clock;

    // 단건 현재가 조회
    public StockCurrentPriceResponse getCurrentPrice(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {

        StockMetadata metadata = stockMetadataCacheService.getMetadata(stockCode);

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

        LocalDateTime updatedAt =
                LocalDateTime.now(clock);


        return new StockCurrentPriceResponse(
                marketType,
                metadata.stockCode(),
                metadata.stockName(),
                output.marketName(),
                output.industryName(),
                output.currentPrice(),
                output.changePrice(),
                output.changeSign(),
                output.changeRate(),
                output.tradeVolume(),
                output.tradeAmount(),
                output.openPrice(),
                output.highPrice(),
                output.lowPrice(),
                updatedAt
        );
    }
}
