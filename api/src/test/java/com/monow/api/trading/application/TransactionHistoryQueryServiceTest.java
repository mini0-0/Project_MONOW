package com.monow.api.trading.application;

import com.monow.api.trading.dto.response.TransactionHistoryDetailResponse;
import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.transaction.entity.TransactionHistoryType;
import com.monow.domain.transaction.repository.TransactionHistoryDetailQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

import com.monow.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TransactionHistoryQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final String STOCK_CODE = "005930";
    private static final String STOCK_NAME = "삼성전자보통주";

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private TransactionHistoryQueryService transactionHistoryQueryService;

    @Nested
    @DisplayName("거래 내역 조회")
    class GetTransactionHistories {

        @Test
        @DisplayName("[성공] - 사용자 ID에 해당하는 거래 내역 조회")
        void getMyTransactionHistories_whenHistoriesExist_returnsTransactionHistories() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            TransactionHistoryQueryResult buyHistory =
                    new TransactionHistoryQueryResult(
                            1L,
                            STOCK_CODE,
                            STOCK_NAME,
                            TransactionHistoryType.BUY,
                            10,
                            BigDecimal.valueOf(3_600_000),
                            BigDecimal.valueOf(100_000_000),
                            BigDecimal.valueOf(96_400_000)
                    );

            TransactionHistoryQueryResult sellHistory =
                    new TransactionHistoryQueryResult(
                            2L,
                            STOCK_CODE,
                            STOCK_NAME,
                            TransactionHistoryType.SELL,
                            5,
                            BigDecimal.valueOf(1_500_000),
                            BigDecimal.valueOf(96_400_000),
                            BigDecimal.valueOf(97_900_000)
                    );
            Page<TransactionHistoryQueryResult> histories = new PageImpl<>(List.of(buyHistory, sellHistory), pageable, 2);

            given(transactionHistoryRepository.findByUserId(USER_ID, pageable))
                    .willReturn(histories);

            // When
            Page<TransactionHistoryListResponse> result = transactionHistoryQueryService.getMyTransactionHistories(USER_ID, pageable);
            log.info("전체 거래 건수 = {}", result.getTotalElements());
            result.getContent().forEach(history -> log.info("거래 내역 = {}", history));

            // Then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            assertThat(result.getContent().get(0).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(3_600_000));
            assertThat(result.getContent().get(1).amount())
                    .isEqualByComparingTo(BigDecimal.valueOf(1_500_000));

            verify(transactionHistoryRepository).findByUserId(USER_ID, pageable);
        }

        @Test
        @DisplayName("[성공] - 사용자에게 거래 내역이 없는 경우 빈 페이지 반환")
        void getMyTransactionHistories_whenHistoriesDoNotExist_returnsEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            Page<TransactionHistoryQueryResult> histories =
                    Page.empty(pageable);

            given(transactionHistoryRepository.findByUserId(USER_ID, pageable))
                    .willReturn(histories);

            // When
            Page<TransactionHistoryListResponse> result = transactionHistoryQueryService.getMyTransactionHistories(USER_ID, pageable);
            log.info("전체 거래 건수 = {}", result.getTotalElements());
            result.getContent().forEach(history -> log.info("거래 내역 = {}", history));

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(transactionHistoryRepository).findByUserId(USER_ID, pageable);

        }
    }

    @Nested
    @DisplayName("거래 내역 상세 조회")
    class GetTransactionHistoryDetail {

        @Test
        @DisplayName("[성공] - 사용자 ID와 거래 내역 ID에 해당하는 거래 내역 상세 조회")
        void getMyTransactionHistoryDetail_whenHistoryExists_returnsDetailResponse() {
            // Given
            Long transactionHistoryId = 1L;
            DomesticStockMarketType marketType = DomesticStockMarketType.KOSPI;
            OrderMarketType orderMarketType = OrderMarketType.KRX;

            LocalDateTime createdAt = LocalDateTime.of(
                    2026,
                    8,
                    18,
                    9,
                    0,
                    16
            );

            TransactionHistoryDetailQueryResult detailHistory = new TransactionHistoryDetailQueryResult(
                    transactionHistoryId,
                    STOCK_CODE,
                    STOCK_NAME,
                    marketType,
                    orderMarketType,
                    TransactionHistoryType.BUY,
                    10,
                    BigDecimal.valueOf(360_000),
                    BigDecimal.valueOf(3_600_000),
                    BigDecimal.valueOf(100_000_000),
                    BigDecimal.valueOf(96_400_000),
                    "삼성전자 10주 매수",
                    createdAt
            );

            given(transactionHistoryRepository.findByTransactionHistoryId(USER_ID, transactionHistoryId))
                    .willReturn(Optional.of(detailHistory));

            // When
            TransactionHistoryDetailResponse response = transactionHistoryQueryService.getMyDetailTransactionHistory(USER_ID, transactionHistoryId);
            log.info("거래 내역 상세 조회 결과 = {}", response);

            // Then
            assertThat(response.transactionHistoryId()).isEqualTo(transactionHistoryId);
            assertThat(response.stockCode()).isEqualTo(STOCK_CODE);
            assertThat(response.transactionHistoryType()).isEqualTo(TransactionHistoryType.BUY);
            assertThat(response.stockMarketType()).isEqualTo(DomesticStockMarketType.KOSPI);
            assertThat(response.orderMarketType()).isEqualTo(OrderMarketType.KRX);
            assertThat(response.quantity()).isEqualTo(10);
            assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(3_600_000));
            assertThat(response.createdAt()).isEqualTo(createdAt);

            verify(transactionHistoryRepository).findByTransactionHistoryId(USER_ID, transactionHistoryId);

        }
        @Test
        @DisplayName("[실패] - 거래 내역이 존재하지 않는 경우 예외 발생")
        void getMyTransactionHistoryDetail_whenHistoryDoesNotExist_throwsException() {
            // Given
            Long transactionHistoryId = 999L;

            given(transactionHistoryRepository.findByTransactionHistoryId(USER_ID, transactionHistoryId))
                    .willReturn(Optional.empty());


            // When & Then
            assertThatThrownBy(() -> transactionHistoryQueryService.getMyDetailTransactionHistory(USER_ID, transactionHistoryId)).isInstanceOf(BusinessException.class);

            verify(transactionHistoryRepository).findByTransactionHistoryId(USER_ID, transactionHistoryId);
        }

    }


}