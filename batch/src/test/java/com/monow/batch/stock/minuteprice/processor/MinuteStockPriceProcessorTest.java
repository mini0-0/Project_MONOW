package com.monow.batch.stock.minuteprice.processor;

import com.monow.batch.stock.minuteprice.application.MinuteStockPriceRetryService;
import com.monow.batch.stock.minuteprice.dto.MinutePriceWindow;
import com.monow.batch.stock.minuteprice.dto.MinuteStockTarget;
import com.monow.batch.stock.minuteprice.mapper.KisMinutePriceMapper;
import com.monow.domain.stock.model.StockPriceMinute;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinuteStockPriceProcessorTest {
    private final MinuteStockPriceRetryService retry = mock(MinuteStockPriceRetryService.class);
    private final MinuteStockTarget target = new MinuteStockTarget(1, "005930");
    private final MinuteStockPriceProcessor processor = new MinuteStockPriceProcessor(retry, new KisMinutePriceMapper(),
            new MinutePriceWindow(LocalDate.of(2026, 11, 11), LocalTime.of(9, 0), LocalTime.of(9, 2)),
            Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    @Test
    @DisplayName("[성공] - 페이지를 역순 조회하고 한국 시간 기준 오름차순 분봉으로 변환")
    void givenMultiplePages_whenProcess_thenMovesCursorAndSortsPrices() {
        when(retry.fetch(any())).thenReturn(response("090200", "090100"), response("090000"));
        var result = processor.process(target);
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(price -> assertThat(price.marketCode()).isEqualTo("UN"));
        assertThat(result.getFirst().barAt().toInstant()).isEqualTo(Instant.parse("2026-11-11T00:00:00Z"));
        ArgumentCaptor<KisMinutePriceRequest> requests = ArgumentCaptor.forClass(KisMinutePriceRequest.class);
        verify(retry, times(2)).fetch(requests.capture());
        assertThat(requests.getAllValues().get(1).inputTime()).isEqualTo(LocalTime.of(9, 0, 59));
    }

    @Test
    @DisplayName("[실패] - 진행되지 않는 페이지 응답을 거부")
    void givenRepeatedPage_whenProcess_thenFailsInsteadOfLooping() {
        when(retry.fetch(any())).thenReturn(response("090200"));
        assertThatThrownBy(() -> processor.process(target)).isInstanceOf(IllegalStateException.class);
        verify(retry, times(2)).fetch(any());
    }

    @Test
    @DisplayName("[실패] - 첫 조회부터 데이터가 없으면 성공 처리하지 않음")
    void givenNoPrices_whenProcess_thenFails() {
        when(retry.fetch(any())).thenReturn(response());
        assertThatThrownBy(() -> processor.process(target)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("[실패] - 잘못된 거래량과 가격 범위를 거부")
    void givenInvalidPrice_whenProcess_thenFails() {
        when(retry.fetch(any())).thenReturn(new KisMinutePriceResponse("0", "OK", "",
                List.of(new KisMinutePriceResponse.Output("20261111", "090000", "100", "99", "98", "100", "10"))));
        assertThatThrownBy(() -> processor.process(target)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[실패] - 요청 날짜 이후의 분봉을 거부")
    void givenFutureDate_whenProcess_thenFails() {
        when(retry.fetch(any())).thenReturn(new KisMinutePriceResponse("0", "OK", "",
                List.of(new KisMinutePriceResponse.Output("20261112", "090000", "100", "100", "100", "100", "10"))));
        assertThatThrownBy(() -> processor.process(target)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"J", "NX"})
    @DisplayName("[실패] - 통합 분봉 저장 모델에서 J와 NX 데이터를 거부")
    void givenNonIntegratedMarket_whenCreatePrice_thenRejectsModel(String market) {
        StockPriceMinute price = new KisMinutePriceMapper().toModel(target, response("090000").output().getFirst(),
                java.time.OffsetDateTime.now(Clock.systemUTC()));

        assertThatThrownBy(() -> new StockPriceMinute(price.stockId(), market, price.barAt(), price.openPrice(),
                price.highPrice(), price.lowPrice(), price.closePrice(), price.volume(), price.collectedAt()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private KisMinutePriceResponse response(String... times) {
        return new KisMinutePriceResponse("0", "OK", "", java.util.Arrays.stream(times).map(time ->
                new KisMinutePriceResponse.Output("20261111", time, "100", "102", "99", "101", "10")).toList());
    }
}
