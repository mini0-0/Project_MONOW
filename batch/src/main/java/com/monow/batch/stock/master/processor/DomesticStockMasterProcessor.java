package com.monow.batch.stock.master.processor;

import com.monow.external.kis.stock.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.stock.client.KisStockInfoClient;
import com.monow.batch.stock.master.dto.DomesticStockMasterItem;
import com.monow.external.kis.stock.dto.response.KisStockInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomesticStockMasterProcessor implements ItemProcessor<DomesticStockMasterItem, Stock> {

    private final StockRepository stockRepository;

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisStockInfoClient kisStockInfoClient;

    private final KisStockInfoMapper kisStockInfoMapper;

    @Override
    public Stock process(
            DomesticStockMasterItem item
    ) {
        String stockCode = item.stockCode();

        if (stockRepository.existsByStockCode(stockCode) == true) {
            return null;
        }
        String accessToken = kisAccessTokenProvider.getAccessToken();

        KisStockInfoResponse stockInfoResponse = kisStockInfoClient.fetchStockInfo(accessToken, stockCode);

        return kisStockInfoMapper.toEntity(
                stockInfoResponse.output(),
                item.marketType(),
                item.krxTradable(),
                item.nxtTradable()
        );
    }

}
