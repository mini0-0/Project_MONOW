package com.monow.batch.stock.dailyprice.processor;

import com.monow.batch.stock.dailyprice.application.DailyStockPriceRetryService;
import com.monow.batch.stock.dailyprice.exception.DailyStockPriceSkippableException;
import com.monow.batch.stock.dailyprice.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.stock.client.KisDailyPriceClient;
import com.monow.external.kis.stock.dto.response.KisDailyPriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DailyStockPriceProcessor implements ItemProcessor<Stock, List<StockPriceDaily>> {

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final DailyStockPriceRetryService dailyStockPriceRetryService;

    private final KisDailyPriceMapper kisDailyPriceMapper;

    private final StockPriceDailyRepository stockPriceDailyRepository;


    public List<StockPriceDaily> process(Stock stock) {
        String stockCode = stock.getStockCode();
        String accessToken = kisAccessTokenProvider.getAccessToken();

        KisDailyPriceResponse response = dailyStockPriceRetryService.fetchDailyPrice(accessToken, stockCode);

        if (response == null || response.output() == null) {
            throw new DailyStockPriceSkippableException(
                    "일별 시세 응답 데이터 누락 stockCode=" + stockCode
            );
        }

        List<StockPriceDaily> dailyPrices;

        try {
            dailyPrices = response.output()
                    .stream()
                    .map(output -> kisDailyPriceMapper.toEntity(stock, output))
                    .toList();


        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new DailyStockPriceSkippableException(
                    "일별 시세 데이터 형식 오류 stockCode=" + stockCode,
                    exception
            );
        }

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
