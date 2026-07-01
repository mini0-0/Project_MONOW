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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        Optional<Watchlist> existWatchlist = watchlistRepository.findByUserAndStock(user, stock);

        if(watchlisted) {
            if (existWatchlist.isEmpty()) {
                Watchlist watchlist = Watchlist.createWatchlist(user, stock);
                watchlistRepository.save(watchlist);
            }
            return true;
        }

        if (existWatchlist.isPresent()) {
            Watchlist watchlist = existWatchlist.get();
            watchlistRepository.delete(watchlist);
        }

        return false;
    }
}
