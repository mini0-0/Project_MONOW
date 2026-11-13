package com.monow.batch.stock.minuteprice.processor;

import com.monow.batch.stock.minuteprice.application.MinuteStockPriceRetryService;
import com.monow.batch.stock.minuteprice.dto.MinutePriceWindow;
import com.monow.batch.stock.minuteprice.dto.MinuteStockTarget;
import com.monow.batch.stock.minuteprice.mapper.KisMinutePriceMapper;
import com.monow.domain.stock.model.StockPriceMinute;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import org.springframework.batch.item.ItemProcessor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MinuteStockPriceProcessor implements ItemProcessor<MinuteStockTarget, List<StockPriceMinute>> {
    private static final int MAX_PAGES = 64;
    private final MinuteStockPriceRetryService retryService;
    private final KisMinutePriceMapper mapper;
    private final MinutePriceWindow window;
    private final Clock clock;

    public MinuteStockPriceProcessor(MinuteStockPriceRetryService retryService, KisMinutePriceMapper mapper,
                                    MinutePriceWindow window, Clock clock) {
        this.retryService = retryService;
        this.mapper = mapper;
        this.window = window;
        this.clock = clock;
    }

    @Override
    public List<StockPriceMinute> process(MinuteStockTarget target) {
        LocalDateTime start = window.tradeDate().atTime(window.startTime());
        LocalDateTime cursor = window.tradeDate().atTime(window.endTime());
        Map<LocalDateTime, StockPriceMinute> result = new TreeMap<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            KisMinutePriceResponse response = retryService.fetch(
                    new KisMinutePriceRequest(target.stockCode(), window.tradeDate(), cursor.toLocalTime()));

            if (response == null || !"0".equals(response.rtCd()) || response.output() == null) {
                throw new IllegalStateException("KIS 분봉 응답이 잘못되었습니다. stockId=" + target.stockId());
            }

            if (response.output().isEmpty()) {
                return requirePrices(result, target);
            }

            OffsetDateTime collectedAt = OffsetDateTime.now(clock);
            LocalDateTime oldest = null;

            for (KisMinutePriceResponse.Output output : response.output()) {
                StockPriceMinute price = mapper.toModel(target, output, collectedAt);
                LocalDateTime time = price.barAt().atZoneSameInstant(MinutePriceWindow.SEOUL).toLocalDateTime();

                if (time.isAfter(cursor)) {
                    throw new IllegalStateException("조회 시각 이후 분봉이 반환되었습니다. stockId=" + target.stockId());
                }

                oldest = oldest == null || time.isBefore(oldest) ? time : oldest;

                if (!time.isBefore(start)) {
                    StockPriceMinute previous = result.putIfAbsent(time, price);
                    if (previous != null && !sameValues(previous, price)) {
                        throw new IllegalStateException("같은 시각의 분봉 값이 충돌합니다. stockId=" + target.stockId());
                    }
                }
            }

            if (oldest == null) {
                throw new IllegalStateException("분봉 조회가 진행되지 않습니다.");
            }
            if (!oldest.isAfter(start)) {
                return requirePrices(result, target);
            }
            cursor = oldest.minusSeconds(1);
        }
        throw new IllegalStateException("분봉 최대 조회 횟수 초과. stockId=" + target.stockId());
    }

    private List<StockPriceMinute> requirePrices(Map<LocalDateTime, StockPriceMinute> result, MinuteStockTarget target) {
        if (result.isEmpty()) {
            throw new IllegalStateException("분봉이 없습니다. 거래일 또는 거래 상태 확인 필요. stockId=" + target.stockId());
        }

        return List.copyOf(result.values());
    }

    private boolean sameValues(StockPriceMinute first, StockPriceMinute second) {
        return first.openPrice().compareTo(second.openPrice()) == 0
                && first.highPrice().compareTo(second.highPrice()) == 0
                && first.lowPrice().compareTo(second.lowPrice()) == 0
                && first.closePrice().compareTo(second.closePrice()) == 0
                && first.volume() == second.volume();
    }
}
