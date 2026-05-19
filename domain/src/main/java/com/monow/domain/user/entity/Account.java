package com.monow.domain.user.entity;

import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "account_number", nullable = false,unique = true, length = 100)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "seed_money", nullable = false, precision = 18, scale = 2)
    private BigDecimal seedMoney;

    private Account(User user, String accountNumber, BigDecimal seedMoney) {
        this.user = user;
        this.accountNumber = accountNumber;
        this.seedMoney = seedMoney;
        this.balance = seedMoney;
        this.status = AccountStatus.ACTIVE;

    }

    public static Account createAccount(User user, String accountNumber, BigDecimal seedMoney) {
        validateSeedMoney(seedMoney);
        return new Account(user, accountNumber, seedMoney);
    }

    private static void validateSeedMoney(BigDecimal seedMoney) {
        if (seedMoney == null || seedMoney.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("초기 지급 금액은 0보다 커야 합니다.");
        }
    }


}
