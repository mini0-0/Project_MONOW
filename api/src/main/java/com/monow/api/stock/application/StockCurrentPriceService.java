package com.monow.api.stock.application;

import com.monow.api.external.kis.application.KisAccessTokenProvider;
import com.monow.api.external.kis.client.KisCurrentPriceClient;
import com.monow.api.external.kis.dto.response.KisCurrentPriceResponse;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCurrentPriceService {

    private final StockRepository stockRepository;

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisCurrentPriceClient kisCurrentPriceClient;

    // 단건 현재가 조회
    public StockCurrentPriceResponse getCurrentPrice(String stockCode, CurrentPriceMarketType marketType) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        String accessToken =  kisAccessTokenProvider.getAccessToken();

        KisCurrentPriceResponse kisResponse = kisCurrentPriceClient.fetchCurrentPrice(
                accessToken,
                stockCode,
                marketType
        );

        if (kisResponse == null || !"0".equals(kisResponse.rtCd())) {
            log.info("KIS 현재가 요청 stockCode={}, marketType={}", stockCode, marketType);
            throw new BusinessException(ErrorCode.KIS_CURRENT_PRICE_FETCH_FAILED);
        }


        KisCurrentPriceResponse.Output output = kisResponse.output();

        if (output == null) {
            throw  new BusinessException(ErrorCode.KIS_CURRENT_PRICE_INVALID_RESPONSE);
        }

        return new StockCurrentPriceResponse(
                stock.getStockCode(),
                stock.getStockName(),
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
                LocalDateTime.now().toString()
        );
    }
}
