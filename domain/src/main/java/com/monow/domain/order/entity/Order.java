package com.monow.domain.order.entity;

import com.monow.domain.account.entity.Account;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.user.entity.User;
import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock  stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_market_type", nullable = false, length = 20)
    private OrderMarketType orderMarketType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "order_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderPrice;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "fail_reason", length = 100)
    private String failReason;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private Order(
            User user,
            Account account,
            Stock stock,
            OrderType orderType,
            OrderStatus orderStatus,
            OrderMarketType orderMarketType,
            Integer quantity,
            BigDecimal orderPrice,
            BigDecimal totalAmount,
            LocalDateTime requestedAt,
            LocalDateTime executedAt
    ) {
        this.user = user;
        this.account = account;
        this.stock = stock;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.orderMarketType = orderMarketType;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.totalAmount = totalAmount;
        this.requestedAt = requestedAt;
        this.executedAt = executedAt;
    }

    public static Order createBuyOrder(
            User user,
            Account account,
            Stock stock,
            OrderMarketType orderMarketType,
            Integer quantity,
            BigDecimal orderPrice,
            BigDecimal totalAmount,
            LocalDateTime requestedAt,
            LocalDateTime executedAt
    ) {
        return new Order(
                user,
                account,
                stock,
                OrderType.BUY,
                OrderStatus.EXECUTED,
                orderMarketType,
                quantity,
                orderPrice,
                totalAmount,
                requestedAt,
                executedAt
        );
    }

    public static Order createSellOrder(
            User user,
            Account account,
            Stock stock,
            OrderMarketType orderMarketType,
            Integer quantity,
            BigDecimal orderPrice,
            BigDecimal totalAmount,
            LocalDateTime requestedAt,
            LocalDateTime executedAt
    ) {
        return new Order(
                user,
                account,
                stock,
                OrderType.SELL,
                OrderStatus.EXECUTED,
                orderMarketType,
                quantity,
                orderPrice,
                totalAmount,
                requestedAt,
                executedAt
        );
    }
}
