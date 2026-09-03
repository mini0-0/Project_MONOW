package com.monow.domain.stock.entity;

import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_number", length = 20)
    private String productNumber;

    @Column(name = "standard_product_number", length = 20)
    private String standardProductNumber;

    @Column(name = "stock_code",nullable = false ,unique = true, length = 20)
    private String stockCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false,length = 30)
    private DomesticStockMarketType marketType;

    @Column(name = "product_type_code", length = 20)
    private String productTypeCode;

    @Column(name = "product_class_code", length = 20)
    private String productClassCode;

    @Column(name = "product_class_name", length = 50)
    private String productClassName;

    @Column(name = "investment_product_type_code", length = 20)
    private String investmentProductTypeCode;

    @Column(name = "investment_product_type_name", length = 50)
    private String investmentProductTypeName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "krx_tradable", nullable = false)
    private Boolean krxTradable;

    @Column(name = "nxt_tradable", nullable = false)
    private Boolean nxtTradable;

    private Stock(
            String productNumber,
            String standardProductNumber,
            String stockCode,
            String productName,
            String stockName,
            DomesticStockMarketType marketType,
            String productTypeCode,
            String productClassCode,
            String productClassName,
            String investmentProductTypeCode,
            String investmentProductTypeName,
            Boolean krxTradable,
            Boolean nxtTradable
    ) {
        this.productNumber = productNumber;
        this.standardProductNumber = standardProductNumber;
        this.stockCode = stockCode;
        this.productName = productName;
        this.stockName = stockName;
        this.marketType = marketType;
        this.productTypeCode = productTypeCode;
        this.productClassCode = productClassCode;
        this.productClassName = productClassName;
        this.investmentProductTypeCode = investmentProductTypeCode;
        this.investmentProductTypeName = investmentProductTypeName;
        this.krxTradable = krxTradable;
        this.nxtTradable = nxtTradable;
        this.isActive = true;
    }

    public static Stock createStock(
            String productNumber,
            String standardProductNumber,
            String stockCode,
            String productName,
            String stockName,
            DomesticStockMarketType marketType,
            String productTypeCode,
            String productClassCode,
            String productClassName,
            String investmentProductTypeCode,
            String investmentProductTypeName,
            Boolean krxTradable,
            Boolean nxtTradable
    ) {
        return new Stock(
                productNumber,
                standardProductNumber,
                stockCode,
                productName,
                stockName,
                marketType,
                productTypeCode,
                productClassCode,
                productClassName,
                investmentProductTypeCode,
                investmentProductTypeName,
                krxTradable,
                nxtTradable
        );
    }
}
