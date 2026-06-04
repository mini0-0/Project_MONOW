package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface StockPriceDailyRepository extends JpaRepository<StockPriceDaily, Long> {
    boolean existsByStockAndTradeDate(Stock stock, LocalDate tradeDate);

    List<StockPriceDaily> findByStockAndTradeDateIn(
            Stock stock,
            Collection<LocalDate> tradeDates
    );

}
