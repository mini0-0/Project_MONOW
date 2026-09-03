package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisDailyPriceClient;
import com.monow.api.external.kis.dto.response.KisDailyPriceResponse;
import com.monow.api.external.kis.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StockPriceSyncService {

    private final StockRepository stockRepository;

    private final StockPriceDailyRepository stockPriceDailyRepository;

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisDailyPriceClient kisDailyPriceClient;

    private final KisDailyPriceMapper kisDailyPriceMapper;

    public void syncDailyPrices() {
        List<Stock> stocks = stockRepository.findAll();

        for (Stock stock : stocks) {
            syncDailyPrice(stock);
            sleep(50);
        }
    }

    private void syncDailyPrice(Stock stock) {
        String stockCode = stock.getStockCode();

        String accessToken = kisAccessTokenProvider.getAccessToken();

        KisDailyPriceResponse response = kisDailyPriceClient.fetchDailyPrice(accessToken, stockCode);

        if (response == null || response.output() == null) {
            throw new BusinessException(ErrorCode.STOCK_DAILY_PRICE_FETCH_FAILED);
        }

        List<StockPriceDaily> dailyPrices = response.output()
                .stream()
                .map(output -> kisDailyPriceMapper.toEntity(stock, output))
                .toList();

        List<LocalDate> tradeDates = dailyPrices.stream()
                .map(StockPriceDaily::getTradeDate)
                .toList();

        Set<LocalDate> existTradeDates = stockPriceDailyRepository
                .findByStockAndTradeDateIn(stock, tradeDates)
                .stream()
                .map(StockPriceDaily::getTradeDate)
                .collect(Collectors.toSet());

        List<StockPriceDaily> newDailyPrices = dailyPrices.stream()
                .filter(dailyPrice -> !existTradeDates.contains(dailyPrice.getTradeDate()))
                .toList();

        stockPriceDailyRepository.saveAll(newDailyPrices);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("일별 시세 동기화 작업이 중단되었습니다.", e);
        }
    }
}
