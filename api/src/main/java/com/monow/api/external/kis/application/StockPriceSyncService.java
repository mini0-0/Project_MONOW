package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisDailyPriceClient;
import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.KisDailyPriceResponse;
import com.monow.api.external.kis.dto.KisTokenResponse;
import com.monow.api.external.kis.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockPriceSyncService {

    private final StockRepository stockRepository;

    private final StockPriceDailyRepository stockPriceDailyRepository;

    private final KisTokenClient kisTokenClient;

    private final KisDailyPriceClient kisDailyPriceClient;

    private final KisDailyPriceMapper kisDailyPriceMapper;

    @Transactional
    public void syncDailyPrice(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        KisTokenResponse tokenResponse = kisTokenClient.issueToken();
        String accessToken = tokenResponse.accessToken();

        KisDailyPriceResponse response = kisDailyPriceClient.fetchDailyPrice(accessToken, stockCode);

        for (KisDailyPriceResponse.Output output: response.output()) {
            StockPriceDaily stockPriceDaily = kisDailyPriceMapper.toEntity(stock, output);

            boolean exists = stockPriceDailyRepository.existsByStockAndTradeDate(
                    stock,
                    stockPriceDaily.getTradeDate()
            );

            if (!exists) {
                stockPriceDailyRepository.save(stockPriceDaily);
            }

        }


    }
}
