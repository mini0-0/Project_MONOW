package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.QStock;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockSearchQueryResult> searchByKeyword(String keyword) {
        QStock stock = QStock.stock;

        List<StockSearchQueryResult> results = queryFactory
                .select(
                        Projections.constructor(
                                StockSearchQueryResult.class,
                                stock.stockCode,
                                stock.stockName,
                                stock.productClassName,
                                stock.marketType,
                                stock.isActive
                    )
                )
                .from(stock)
                .where(
                        stock.productClassName.eq("주권")
                                .and( stock.isActive.eq(true))
                                .and( stock.stockName.containsIgnoreCase(keyword).or(stock.stockCode.eq(keyword))
                                )
                )
                .fetch();

        return results;

    }

}
