package com.monow.batch.stock.minuteprice.mapper;

import com.monow.batch.stock.minuteprice.dto.MinuteStockTarget;
import com.monow.domain.stock.model.StockPriceMinute;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import com.monow.external.kis.type.CurrentPriceMarketType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class KisMinutePriceMapper {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss")
            .withResolverStyle(ResolverStyle.STRICT);

    public StockPriceMinute toModel(MinuteStockTarget target, KisMinutePriceResponse.Output output,
                                   OffsetDateTime collectedAt) {
        LocalDate date = LocalDate.parse(output.tradeDate(), DateTimeFormatter.BASIC_ISO_DATE);
        LocalTime time = LocalTime.parse(output.tradeTime(), TIME_FORMAT);

        return new StockPriceMinute(target.stockId(), CurrentPriceMarketType.INTEGRATED.getKisCode(),
                date.atTime(time).atZone(SEOUL).toOffsetDateTime(),
                new BigDecimal(output.openPrice()), new BigDecimal(output.highPrice()),
                new BigDecimal(output.lowPrice()), new BigDecimal(output.closePrice()),
                Long.parseLong(output.volume()), collectedAt);
    }
}
