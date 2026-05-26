package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockId);

    Boolean existsByStockCode(String stockCode);
}
