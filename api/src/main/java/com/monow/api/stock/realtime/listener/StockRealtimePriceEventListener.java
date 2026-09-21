package com.monow.api.stock.realtime.listener;

import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.api.stock.realtime.handler.StockRealtimePriceHandler;
import com.monow.external.kis.realtime.event.KisRealtimePriceReceivedEvent;
import com.monow.external.kis.realtime.model.KisRealtimePriceData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockRealtimePriceEventListener {

    private final StockRealtimePriceHandler stockRealtimePriceHandler;

    @EventListener
    public void handleRealtimePrice(KisRealtimePriceReceivedEvent event) {
        KisRealtimePriceData data = event.data();

        StockRealtimePriceResponse response = StockRealtimePriceResponse.from(data);

        stockRealtimePriceHandler.handleRealtimePrice(
                response.marketType(),
                response.stockCode(),
                response
        );
    }


}
