package com.monow.domain.holding.repository;

import com.monow.domain.holding.entity.QHolding;
import com.monow.domain.stock.entity.QStock;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class HoldingRepositoryImpl implements HoldingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PortfolioHoldingQueryResult> findPortfolioHoldings(Long userId) {
        QHolding holding = QHolding.holding;
        QStock stock = QStock.stock;

        List<PortfolioHoldingQueryResult> results = queryFactory
                .select(
                        Projections.constructor(
                                PortfolioHoldingQueryResult.class,
                                stock.stockCode,
                                stock.stockName,
                                stock.marketType,
                                holding.quantity,
                                holding.totalPurchaseAmount
                        )
                )
                .from(holding)
                .join(holding.stock, stock)
                .where(holding.user.id.eq(userId))
                .fetch();

        return results;
    }

}
