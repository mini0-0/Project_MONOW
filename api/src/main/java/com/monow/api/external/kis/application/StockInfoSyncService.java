package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisStockInfoClient;
import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import com.monow.api.external.kis.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockInfoSyncService {

    private final StockRepository stockRepository;

    private final KisTokenClient kisTokenClient;

    private final KisStockInfoClient kisStockInfoClient;

    private final KisStockInfoMapper kisStockInfoMapper;

    @Transactional
    public void syncStockInfo(String stockCode) {

        if (stockRepository.existsByStockCode(stockCode)) {
            return;
        }

        KisTokenResponse tokenResponse = kisTokenClient.issueToken();
        String accessToken = tokenResponse.accessToken();

        KisStockInfoResponse response = kisStockInfoClient.fetchStockInfo(accessToken, stockCode);

        if (response == null || !"0".equals(response.rtCd())) {
            throw new BusinessException(ErrorCode.STOCK_INFO_FETCH_FAILED);
        }

        if (response.output() == null) {
            throw new BusinessException(ErrorCode.STOCK_INFO_FETCH_FAILED);
        }

        KisStockInfoResponse.Output output = response.output();

        Stock stock = kisStockInfoMapper.toEntity(output);

        stockRepository.save(stock);

    }
}
