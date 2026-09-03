package com.monow.api.stock.application.currentprice;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class StockCurrentPriceMarketSelector {

    private final Clock clock;

    private static final LocalTime NXT_PRE_MARKET_START = LocalTime.of(8, 0);

    public static final LocalTime NXT_PRE_MARKET_END = LocalTime.of(8,50);

    public static final LocalTime KRX_MARKET_START = LocalTime.of(9,0);
    private static final LocalTime KRX_MARKET_END = LocalTime.of(15, 30);

    private static final LocalTime INTEGRATED_MARKET_START = LocalTime.of(9, 0, 30);

    private static final LocalTime INTEGRATED_MARKET_END = LocalTime.of(15, 20);

    private static final LocalTime NXT_AFTER_MARKET_START = LocalTime.of(15, 40);

    private static final LocalTime NXT_AFTER_MARKET_END = LocalTime.of(20, 0);

    public StockCurrentPriceMarketSelection select() {
        LocalTime now = LocalTime.now(clock);

        // NXT 프리마켓 거래 시간: 08:00 ~ 08:50
        if (isBetween(now, NXT_PRE_MARKET_START, NXT_PRE_MARKET_END)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.NXT,
                    StockMarketStatus.PRE_MARKET,
                    true
            );
        }

        // NXT 프리마켓 종료 후 KRX 정규시장 시작 전: 08:50 ~ 09:00
        if (isBetween(now, NXT_PRE_MARKET_END, KRX_MARKET_START)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.NXT,
                    StockMarketStatus.TRANSITION,
                    false
            );
        }

        // KRX 단독 거래 시간: 09:00 ~ 09:00:30
        if (isBetween(now, KRX_MARKET_START, INTEGRATED_MARKET_START)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.OPEN,
                    true
            );
        }

        // KRX + NXT 동시 거래 시간: 09:00:30 ~ 15:20
        if (isBetween(now, INTEGRATED_MARKET_START, INTEGRATED_MARKET_END)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.INTEGRATED,
                    StockMarketStatus.OPEN,
                    true
            );
        }

        // NXT 메인마켓 종료 후 KRX 단독 거래 시간: 15:20 ~ 15:30
        if (isBetween(now, INTEGRATED_MARKET_END, KRX_MARKET_END)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.OPEN,
                    true
            );
        }

        // KRX 종료 후 NXT 애프터마켓 시작 전: 15:30 ~ 15:40
        if (isBetween(now, KRX_MARKET_END, NXT_AFTER_MARKET_START)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.TRANSITION,
                    false
            );
        }

        // NXT 애프터마켓 거래 시간: 15:40 ~ 20:00
        if (isBetween(now, NXT_AFTER_MARKET_START, NXT_AFTER_MARKET_END)) {
            return new StockCurrentPriceMarketSelection(
                    CurrentPriceMarketType.NXT,
                    StockMarketStatus.AFTER_MARKET,
                    true
            );
        }

        // 전체 시장 종료 시간: 20:00 ~ 다음날 08:00
        return new StockCurrentPriceMarketSelection(
                CurrentPriceMarketType.NXT,
                StockMarketStatus.CLOSED,
                false
        );
    }

    private boolean isBetween(
            LocalTime now,
            LocalTime start,
            LocalTime end
    ) {
        return !now.isBefore(start) && now.isBefore(end);
    }


}
