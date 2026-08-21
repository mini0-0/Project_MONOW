package com.monow.domain.transaction.repository;

import com.monow.domain.order.entity.QOrder;
import com.monow.domain.stock.entity.QStock;
import com.monow.domain.transaction.entity.QTransactionHistory;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<TransactionHistoryQueryResult> findByUserId(Long userId, Pageable pageable) {

        QTransactionHistory transactionHistory = QTransactionHistory.transactionHistory;

        QOrder order = QOrder.order;

        QStock stock = QStock.stock;

        // 사용자 건수
        List<TransactionHistoryQueryResult> content = queryFactory
                .select(
                        Projections.constructor(
                                TransactionHistoryQueryResult.class,
                                transactionHistory.id,
                                stock.stockCode,
                                stock.stockName,
                                transactionHistory.transactionHistoryType,
                                order.quantity,
                                transactionHistory.amount,
                                transactionHistory.beforeBalance,
                                transactionHistory.afterBalance
                        )
                )
                .from(transactionHistory)
                .join(transactionHistory.order, order)
                .join(order.stock, stock)
                .where(transactionHistory.user.id.eq(userId))
                .orderBy(transactionHistory.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수
        JPAQuery<Long> countQuery = queryFactory
                .select(transactionHistory.count())
                .from(transactionHistory)
                .where(transactionHistory.user.id.eq(userId));


        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );

    }

    @Override
    public Optional<TransactionHistoryDetailQueryResult> findByTransactionHistoryId(Long userId, Long transactionHistoryId) {

        QTransactionHistory transactionHistory = QTransactionHistory.transactionHistory;

        QOrder order = QOrder.order;

        QStock stock = QStock.stock;

        TransactionHistoryDetailQueryResult result = queryFactory
                .select(
                        Projections.constructor(
                                TransactionHistoryDetailQueryResult.class,
                                transactionHistory.id,
                                stock.stockCode,
                                stock.stockName,
                                stock.marketType,
                                order.orderMarketType,
                                transactionHistory.transactionHistoryType,
                                order.quantity, order.orderPrice,
                                transactionHistory.amount,
                                transactionHistory.beforeBalance,
                                transactionHistory.afterBalance,
                                transactionHistory.description,
                                transactionHistory.createdAt
                        )
                )
                .from(transactionHistory)
                .join(transactionHistory.order, order)
                .join(order.stock, stock)
                .where(transactionHistory.user.id.eq(userId), transactionHistory.id.eq(transactionHistoryId))
                .fetchOne();

        return Optional.ofNullable(result);

    }

}
