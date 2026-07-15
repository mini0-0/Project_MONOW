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
class WatchlistServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    @Nested
    @DisplayName("관심종목 등록")
    class AddWatchlist {

        @Test
        @DisplayName("[성공] - 미등록 종목을 관심종목으로 설정하면 관심종목 저장")
        void setWatchlistStatus_whenWatchlistedTrueAndNotExists_savesWatchlist() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";
            User user = createUser();
            Stock stock = createStock(stockCode);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.existsByUserAndStock(user, stock))
                    .willReturn(false);

            // When
            boolean result = watchlistService.setWatchlistStatus(
                    userId,
                    stockCode,
                    true
            );

            // Then
            assertThat(result).isTrue();

            verify(watchlistRepository)
                    .existsByUserAndStock(user, stock);

            verify(watchlistRepository, times(1))
                    .save(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .findByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));
        }

        @Test
        @DisplayName("[성공] - 이미 등록된 종목을 관심종목으로 설정하면 중복 저장 생략")
        void setWatchlistStatus_whenWatchlistedTrueAndAlreadyExists_doesNotSaveAgain() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";
            User user = createUser();
            Stock stock = createStock(stockCode);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.existsByUserAndStock(user, stock))
                    .willReturn(true);

            // When
            boolean result = watchlistService.setWatchlistStatus(
                    userId,
                    stockCode,
                    true
            );

            // Then
            assertThat(result).isTrue();

            verify(watchlistRepository)
                    .existsByUserAndStock(user, stock);

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .findByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));
        }
    }

    @Nested
    @DisplayName("관심종목 해제")
    class RemoveWatchlist {

        @Test
        @DisplayName("[성공] - 등록된 종목을 관심종목 해제로 설정하면 관심종목 삭제")
        void setWatchlistStatus_whenWatchlistedFalseAndExists_deletesWatchlist() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";
            User user = createUser();
            Stock stock = createStock(stockCode);
            Watchlist watchlist = Watchlist.createWatchlist(user, stock);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.findByUserAndStock(user, stock))
                    .willReturn(Optional.of(watchlist));

            // When
            boolean result = watchlistService.setWatchlistStatus(
                    userId,
                    stockCode,
                    false
            );

            // Then
            assertThat(result).isFalse();

            verify(watchlistRepository)
                    .findByUserAndStock(user, stock);

            verify(watchlistRepository)
                    .delete(watchlist);

            verify(watchlistRepository, never())
                    .existsByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));
        }

        @Test
        @DisplayName("[성공] - 이미 해제된 종목을 관심종목 해제로 설정하면 삭제 생략")
        void setWatchlistStatus_whenWatchlistedFalseAndNotExists_doesNotDelete() {
            // Given
            Long userId = 1L;
            String stockCode = "005930";
            User user = createUser();
            Stock stock = createStock(stockCode);

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(watchlistRepository.findByUserAndStock(user, stock))
                    .willReturn(Optional.empty());

            // When
            boolean result = watchlistService.setWatchlistStatus(
                    userId,
                    stockCode,
                    false
            );

            // Then
            assertThat(result).isFalse();

            verify(watchlistRepository)
                    .findByUserAndStock(user, stock);

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .existsByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));
        }
    }

    @Nested
    @DisplayName("관심종목 상태 변경 실패")
    class SetWatchlistStatusFail {

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
                    () -> watchlistService.setWatchlistStatus(
                            userId,
                            stockCode,
                            true
                    )
            );

            // Then
            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(stockRepository, never())
                    .findByStockCode(anyString());

            verify(watchlistRepository, never())
                    .existsByUserAndStock(any(User.class), any(Stock.class));

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
            User user = createUser();

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.empty());

            // When
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> watchlistService.setWatchlistStatus(
                            userId,
                            stockCode,
                            true
                    )
            );

            // Then
            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.STOCK_NOT_FOUND);

            verify(watchlistRepository, never())
                    .existsByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .findByUserAndStock(any(User.class), any(Stock.class));

            verify(watchlistRepository, never())
                    .save(any(Watchlist.class));

            verify(watchlistRepository, never())
                    .delete(any(Watchlist.class));
        }
    }

    private User createUser() {
        return User.createUser(
                "test@test.com",
                "1234",
                "홍길동",
                "워렌버핏"
        );
    }

    private Stock createStock(String stockCode) {
        return Stock.createStock(
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
    }
}