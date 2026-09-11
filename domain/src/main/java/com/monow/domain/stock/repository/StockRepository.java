package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long>, StockRepositoryCustom {
    List<Stock> findByStockCodeIn(Collection<String> stockCodes);

    Optional<Stock> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    long countByStockCode(String stockCode);

    List<Stock> findByStockCodeInAndProductClassCodeNot(
            List<String> stockCodes,
            String productClassCode
    );

}
