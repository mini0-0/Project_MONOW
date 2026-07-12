package com.monow.api.stock.application;

import com.monow.api.stock.dto.StockMetadata;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class StockMetadataCacheService {

    private final Map<String, StockMetadata> metadataCache =
            new ConcurrentHashMap<>();

    private final StockRepository stockRepository;

    public StockMetadata getMetadata(String stockCode) {
        validateStockCode(stockCode);

        return metadataCache.computeIfAbsent(
                stockCode,
                this::loadMetadata
        );
    }

    private StockMetadata loadMetadata(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        return new StockMetadata(
                stock.getStockCode(),
                stock.getStockName()
        );
    }

    public void evict(String stockCode) {
        validateStockCode(stockCode);

        metadataCache.remove(stockCode);
    }

    public void clear() {
        metadataCache.clear();
    }

    private void validateStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException(
                    "종목 코드는 필수입니다."
            );
        }
    }

}
