package com.monow.batch.stock.dailyprice.mapper;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.external.kis.stock.dto.response.KisDailyPriceResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class KisDailyPriceMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public StockPriceDaily toEntity(Stock stock, KisDailyPriceResponse.Output output) {
        LocalDate tradeDate = LocalDate.parse(
                output.tradeDate(),
                DATE_TIME_FORMATTER
        );
        BigDecimal openPrice = new BigDecimal(output.openPrice());
        BigDecimal highPrice = new BigDecimal(output.highPrice());
        BigDecimal lowPrice = new BigDecimal(output.lowPrice());
        BigDecimal closePrice = new BigDecimal(output.closePrice());
        Long volume = Long.parseLong(output.volume());

        return StockPriceDaily.createDailyPrice(
                stock,
                tradeDate,
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume

        );
    }
}
