package com.monow.domain.watchlist.entity;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.user.entity.User;
import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "watchlists",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_watchlist_user_stock",
                        columnNames = {"user_id", "stock_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    private Watchlist(User user, Stock stock) {
        this.user = user;
        this.stock = stock;
    }

    public static Watchlist createWatchlist(User user, Stock stock) {
        return new Watchlist(user, stock);
    }
}
