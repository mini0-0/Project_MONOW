package com.monow.api.trading.application;

import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import com.monow.domain.transaction.entity.TransactionHistoryType;
import com.monow.domain.transaction.repository.TransactionHistoryQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;

import java.util.List;

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


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TransactionHistoryQueryServiceTest {

    private static final Long USER_ID = 2L;
    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private TransactionHistoryQueryService transactionHistoryQueryService;

    @Nested
    @DisplayName("거래 내역 조회")
    class GetTransactionHistories {

        @Test
        @DisplayName("[성공] - 사용자 ID에 해당하는 거래 내역 조회")
        void getMyTransactionHistories_success() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            TransactionHistoryQueryResult buyHistory =
                    new TransactionHistoryQueryResult(
                            1L,
                            "005930",
                            "삼성전자보통주",
                            TransactionHistoryType.BUY,
                            10,
                            BigDecimal.valueOf(3_600_000),
                            BigDecimal.valueOf(100_000_000),
                            BigDecimal.valueOf(96_400_000)
                    );

            TransactionHistoryQueryResult sellHistory =
                    new TransactionHistoryQueryResult(
                            2L,
                            "005930",
                            "삼성전자보통주",
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
        void getMyTransactionHistories_empty() {
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

}