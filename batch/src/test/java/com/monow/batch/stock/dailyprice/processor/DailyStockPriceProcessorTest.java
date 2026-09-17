package com.monow.batch.stock.dailyprice.processor;

import com.monow.batch.stock.dailyprice.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.external.kis.application.KisAccessTokenProvider;
import com.monow.global.external.kis.client.KisDailyPriceClient;
import com.monow.global.external.kis.dto.response.KisDailyPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyStockPriceProcessorTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";

    @InjectMocks
    private DailyStockPriceProcessor dailyStockPriceProcessor;

    @Mock
    private KisAccessTokenProvider kisAccessTokenProvider;

    @Mock
    private KisDailyPriceClient kisDailyPriceClient;

    @Mock
    private KisDailyPriceMapper kisDailyPriceMapper;

    @Mock
    private StockPriceDailyRepository stockPriceDailyRepository;

    @Test
    @DisplayName("[성공] - 기존 거래일을 제외하고 신규 일별 시세만 반환")
    void process_whenExistingDailyPriceExists_returnsOnlyNewDailyPrice() throws Exception {
        // Given
        Stock stock = createStock();

        KisDailyPriceResponse.Output existingOutput = new KisDailyPriceResponse.Output(
                "20260909",
                "71000",
                "72500",
                "70000",
                "71800",
                "11111111"
        );

        KisDailyPriceResponse.Output newOutput = new KisDailyPriceResponse.Output(
                "20260910",
                "72000",
                "73500",
                "71000",
                "72800",
                "12345678"
        );

        KisDailyPriceResponse kisResponse = new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                List.of(existingOutput, newOutput)
        );

        LocalDate existingTradeDate  = LocalDate.of(2026, 9, 9);
        LocalDate newTradeDate = LocalDate.of(2026, 9, 10);

        StockPriceDaily existingDailyPrice = StockPriceDaily.createDailyPrice(
                stock,
                existingTradeDate,
                new BigDecimal("71000"),
                new BigDecimal("72500"),
                new BigDecimal("70000"),
                new BigDecimal("71800"),
                11111111L
        );

        StockPriceDaily newDailyPrice = StockPriceDaily.createDailyPrice(
                stock,
                newTradeDate,
                new BigDecimal("72000"),
                new BigDecimal("73500"),
                new BigDecimal("71000"),
                new BigDecimal("72800"),
                12345678L
        );

        given(kisDailyPriceMapper.toEntity(stock, existingOutput))
                .willReturn(existingDailyPrice);
        given(kisDailyPriceMapper.toEntity(stock, newOutput))
                .willReturn(newDailyPrice);

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);

        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .willReturn(kisResponse);
        given(stockPriceDailyRepository.findByStockAndTradeDateIn(stock, List.of(existingTradeDate, newTradeDate)))
                .willReturn(List.of(existingDailyPrice));

        // When
        List<StockPriceDaily> result = dailyStockPriceProcessor.process(stock);


        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(newDailyPrice);

        verify(kisAccessTokenProvider, times(1))
                .getAccessToken();
        verify(kisDailyPriceClient, times(1))
                .fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);
        verify(kisDailyPriceClient, times(1))
                .fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);


    }

    @Test
    @DisplayName("[실패] - KIS 일별 시세 응답이 null이면 예외 발생")
    void process_whenKisResponseIsNull_throwsException() throws Exception {
        // Given
        Stock stock = createStock();

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .willReturn(null);

        // When & Then
        assertThatThrownBy(() -> dailyStockPriceProcessor.process(stock))
                .isInstanceOf(BusinessException.class);

        verify(kisAccessTokenProvider, times(1))
                .getAccessToken();
        verify(kisDailyPriceClient, times(1))
                .fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);

        verifyNoInteractions(kisDailyPriceMapper);
        verifyNoInteractions(stockPriceDailyRepository);

    }


    @Test
    @DisplayName("[실패] - KIS 일별 시세 output이 null이면 예외 발생")
    void process_whenKisOutputIsNull_throwsException() throws Exception {
        // Given
        Stock stock = createStock();

        KisDailyPriceResponse kisResponse = new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                null
        );

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);

        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .willReturn(kisResponse);

        // When & Then
        assertThatThrownBy(() -> dailyStockPriceProcessor.process(stock))
                .isInstanceOf(BusinessException.class);

        verify(kisAccessTokenProvider, times(1))
                .getAccessToken();
        verify(kisDailyPriceClient, times(1))
                .fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);

        verifyNoInteractions(kisDailyPriceMapper);
        verifyNoInteractions(stockPriceDailyRepository);


    }

    private Stock createStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                STOCK_CODE,
                "삼성전자보통주",
                "삼성전자",
                DomesticStockMarketType.KOSPI,
                "300",
                "101010",
                "주권",
                "1010",
                "주식",
                true,
                true
        );
    }

}