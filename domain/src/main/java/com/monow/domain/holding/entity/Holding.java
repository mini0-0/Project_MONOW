package com.monow.domain.holding.entity;

import com.monow.domain.account.entity.Account;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.user.entity.User;
import com.monow.global.common.entity.BaseTimeEntity;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(
        name = "holdings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_holding_account_stock",
                        columnNames = {"account_id", "stock_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding extends BaseTimeEntity {

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
    private Stock stock;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_purchase_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPurchaseAmount;

    private Holding(
            User user,
            Account account,
            Stock stock,
            Integer quantity,
            BigDecimal totalPurchaseAmount
    ) {
        this.user = user;
        this.account = account;
        this.stock = stock;
        this.quantity = quantity;
        this.totalPurchaseAmount = totalPurchaseAmount;
    }

    public static Holding createHolding(
            User user,
            Account account,
            Stock stock,
            Integer quantity,
            BigDecimal totalPurchaseAmount
    ) {
        return new Holding(
                user,
                account,
                stock,
                quantity,
                totalPurchaseAmount
        );

    }

    public BigDecimal calculateAverageBuyPrice() {
        return totalPurchaseAmount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public void buy(int purchaseQuantity, BigDecimal purchasePrice) {
        validateQuantity(purchaseQuantity);
        validatePurchasePrice(purchasePrice);

        // 이번 추가 매수의 총 매입금액
        BigDecimal additionalPurchaseAmount  = purchasePrice.multiply(BigDecimal.valueOf(purchaseQuantity));

        // 추가 매수 후 전체 보유 수량
        int newQuantity = quantity + purchaseQuantity;

        // 추가 매수 후 현재 보유 종목의 총 매입금액
        BigDecimal newTotalPurchaseAmount = totalPurchaseAmount.add(additionalPurchaseAmount);

        this.quantity = newQuantity;
        this.totalPurchaseAmount = newTotalPurchaseAmount;

    }

    public void sell(int sellQuantity) {
        validateQuantity(sellQuantity);

        if (quantity < sellQuantity) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_QUANTITY);
        }

        // 전량 매도
        if (quantity == sellQuantity) {
            this.quantity = 0;
            this.totalPurchaseAmount = BigDecimal.ZERO;
            return;
        }

        // 매도 전 평균 매입가 계산
        BigDecimal averageBuyPrice = calculateAverageBuyPrice();

        // 이번에 매도하는 수량에 해당하는 기존 매입 원가
        BigDecimal soldPurchaseAmount = averageBuyPrice.multiply(BigDecimal.valueOf(sellQuantity));

        // 매도 후 남은 보유 수량
        int newQuantity = quantity - sellQuantity;

        // 매도 후 남은 주식의 총 매입 원가
        BigDecimal newTotalPurchaseAmount = totalPurchaseAmount.subtract(soldPurchaseAmount);

        this.quantity = newQuantity;
        this.totalPurchaseAmount = newTotalPurchaseAmount;


    }

    // 매수 및 매도 수량 공통 검증
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_QUANTITY);
        }

    }

    // 매수 가격 검증
    private void validatePurchasePrice(BigDecimal purchasePrice) {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_PRICE);
        }
    }

}
