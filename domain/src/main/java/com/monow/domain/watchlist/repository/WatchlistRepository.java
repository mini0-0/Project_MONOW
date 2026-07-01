package com.monow.domain.watchlist.repository;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.user.entity.User;
import com.monow.domain.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    Optional<Watchlist> findByUserAndStock(User user, Stock stock);
}
