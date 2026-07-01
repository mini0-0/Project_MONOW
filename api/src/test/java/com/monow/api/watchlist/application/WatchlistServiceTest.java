package com.monow.api.watchlist.application;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;

import com.monow.domain.user.entity.User;
import com.monow.domain.user.repository.UserRepository;
import com.monow.domain.watchlist.entity.Watchlist;
import com.monow.domain.watchlist.repository.WatchlistRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatchlistServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @InjectMocks
    private WatchlistService  watchlistService;


    @Nested
    @DisplayName("관심 종목 상태 변경")
    class AddWatchlist {

        @Test
        @DisplayName("[성공] - 사용자가 관심목록을 추가하면 관심목록이 저장")
        void addWatchlist_whenUserAndStockExist_savesWatchlist() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";

            User user = User.createUser(
                    "test@test.com",
                    "1234",
                    "홍길동",
                    "워렌버핏"
                );

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
                );


            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.findByUserAndStock(user, stock))
                    .willReturn(Optional.empty());

            // When
            boolean result = watchlistService.setWatchlistStatus(userId, stockCode, true);


            // Then
            assertThat(result).isTrue();
            verify(watchlistRepository, times(1))
                    .save(any(Watchlist.class));

        }

        @Test
        @DisplayName("[성공] - 이미 등록된 관심종목이면 관심종목 취소")
        void addWatchlist_whenWatchlistAlreadyExists_throwsException() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";

            User user = User.createUser(
                    "test@test.com",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            Watchlist existWatchlist = Watchlist.createWatchlist(user, stock);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.findByUserAndStock(user, stock))
                    .willReturn(Optional.of(existWatchlist));

            // When
            boolean result = watchlistService.setWatchlistStatus(userId, stockCode, false);

            // Then
            assertThat(result).isFalse();
            verify(watchlistRepository, times(1))
                    .delete(existWatchlist);
            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));

        }


        @Test
        @DisplayName("[실패] - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void setWatchlistStatus_whenUserNotFound_throwsException() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";

            given(userRepository.findById(userId))
                    .willReturn(Optional.empty());

            // When
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> watchlistService.setWatchlistStatus(userId, stockCode, true)
            );

            // Then
            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);


            verify(stockRepository, never())
                    .findByStockCode(anyString());

            verify(watchlistRepository, never())
                    .findByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));
        }

        @Test
        @DisplayName("[실패] - 종목이 존재하지 않으면 STOCK_NOT_FOUND 예외 발생")
        void setWatchlistStatus_whenStockNotFound_throwsException() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";

            User user = User.createUser(
                    "test@test.com",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.empty());

            // When
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> watchlistService.setWatchlistStatus(userId, stockCode, true)
            );

            // Then
            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.STOCK_NOT_FOUND);

            verify(watchlistRepository, never())
                    .findByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));
        }
    }


}
