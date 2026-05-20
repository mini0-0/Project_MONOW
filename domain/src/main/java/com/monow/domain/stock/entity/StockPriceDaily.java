package com.monow.domain.stock.entity;

import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_price_daily")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPriceDaily extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "trade_date", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false)
    private Long volume;

}
