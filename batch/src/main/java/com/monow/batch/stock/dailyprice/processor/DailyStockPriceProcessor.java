package com.monow.batch.stock.dailyprice.processor;

import com.monow.batch.stock.dailyprice.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import com.monow.global.external.kis.application.KisAccessTokenProvider;
import com.monow.global.external.kis.client.KisDailyPriceClient;
import com.monow.global.external.kis.dto.response.KisDailyPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DailyStockPriceProcessor implements ItemProcessor<Stock, List<StockPriceDaily>> {

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisDailyPriceClient kisDailyPriceClient;

    private final KisDailyPriceMapper kisDailyPriceMapper;

    private final StockPriceDailyRepository stockPriceDailyRepository;


    public List<StockPriceDaily> process(Stock stock) {
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

        return newDailyPrices;
    }
}
