package com.monow.domain.transaction.entity;

import com.monow.domain.account.entity.Account;
import com.monow.domain.order.entity.Order;
import com.monow.domain.user.entity.User;
import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionHistory extends BaseTimeEntity {

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
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_history_type", nullable = false, length = 50)
    private TransactionHistoryType transactionHistoryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "before_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal beforeBalance;

    @Column(name = "after_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal afterBalance;

    @Column(length = 255)
    private String description;

    private TransactionHistory(
            User user,
            Account account,
            Order order,
            TransactionHistoryType transactionHistoryType,
            BigDecimal amount,
            BigDecimal beforeBalance,
            BigDecimal afterBalance,
            String description
    ) {
        this.user = user;
        this.account = account;
        this.order = order;
        this.transactionHistoryType = transactionHistoryType;
        this.amount = amount;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.description = description;
    }

    public static TransactionHistory createBuyHistory(
            User user,
            Account account ,
            Order order,
            BigDecimal amount,
            BigDecimal beforeBalance,
            BigDecimal afterBalance,
            String description
    ) {
        return new TransactionHistory(
                user,
                account,
                order,
                TransactionHistoryType.BUY,
                amount,
                beforeBalance,
                afterBalance,
                description
        );

    }
}
