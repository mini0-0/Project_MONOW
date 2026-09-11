package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.QStock;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StockSearchQueryResult> searchByKeyword(String keyword, Pageable pageable) {
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
                                .and(
                                        stock.stockName.startsWithIgnoreCase(keyword)
                                                .or(stock.stockCode.startsWith(keyword))
                                )
                )
                .orderBy(
                        new CaseBuilder()
                                .when(stock.stockName.eq(keyword)
                                        .or(stock.stockCode.eq(keyword))
                                )
                                .then(0)
                                .otherwise(1)
                                .asc(),
                        stock.stockName.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(stock.count())
                .from(stock)
                .where(
                        stock.productClassName.eq("주권")
                                .and(stock.isActive.eq(true))
                                .and(
                                        stock.stockName.startsWith(keyword)
                                                .or(stock.stockCode.startsWith(keyword))
                                )
                )
                .fetchOne();

        return new PageImpl<>(
                results,
                pageable,
                total != null ? total : 0L
        );
    }

}
