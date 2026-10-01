package com.monow.batch.stock.master.processor;

import com.monow.batch.stock.master.application.DomesticStockMasterRetryService;
import com.monow.batch.stock.master.exception.DomesticStockMasterSkippableException;
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

    private final DomesticStockMasterRetryService domesticStockMasterRetryService;

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

        KisStockInfoResponse stockInfoResponse = domesticStockMasterRetryService.fetchDomesticStockMaster(accessToken, stockCode);

        if (stockInfoResponse == null || stockInfoResponse.output() == null) {
            throw new DomesticStockMasterSkippableException(
                    "종목 Master 응답 데이터 누락 stockCode=" + stockCode
            );
        }

        try {
            return kisStockInfoMapper.toEntity(
                    stockInfoResponse.output(),
                    item.marketType(),
                    item.krxTradable(),
                    item.nxtTradable()
            );
        } catch (IllegalArgumentException exception) {
            throw new DomesticStockMasterSkippableException(
                    "종목 Master 데이터 변환 오류 stockCode=" + stockCode,
                    exception
            );
        }


    }

}
