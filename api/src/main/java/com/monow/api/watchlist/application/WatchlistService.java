package com.monow.api.watchlist.application;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.user.entity.User;
import com.monow.domain.user.repository.UserRepository;
import com.monow.domain.watchlist.entity.Watchlist;
import com.monow.domain.watchlist.repository.WatchlistRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final UserRepository userRepository;

    private final StockRepository stockRepository;

    private final WatchlistRepository watchlistRepository;

    @Transactional
    public boolean setWatchlistStatus(Long userId, String stockCode, boolean watchlisted) {
        User user = getUser(userId);
        Stock stock = getStock(stockCode);

        if (watchlisted) {
            addWatchlist(user, stock);
            return true;
        }

        removeWatchlist(user, stock);
        return false;
    }

    private void addWatchlist(User user, Stock stock) {
        boolean watchlisted = watchlistRepository.existsByUserAndStock(user, stock);

        if (watchlisted) {
            return;
        }
        Watchlist watchlist = Watchlist.createWatchlist(user, stock);

        watchlistRepository.save(watchlist);

    }

    private void removeWatchlist(User user, Stock stock) {
        watchlistRepository.findByUserAndStock(user, stock)
                .ifPresent(watchlistRepository::delete);

    }


    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }


    private Stock getStock(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
    }
}
