package com.monow.batch.stock.minuteprice.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinutePriceWindowTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T00:00:00Z"), MinutePriceWindow.SEOUL);

    @Test
    @DisplayName("[성공] - UN 통합 일중 수집 구간을 생성")
    void givenIntegratedDay_whenCreateWindow_thenAcceptsExplicitRange() {
        MinutePriceWindow window = MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "08:00", "20:00"), clock);

        assertThat(window.tradeDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(window.startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(window.endTime()).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    @DisplayName("[성공] - 특별 거래일의 명시적 수집 구간을 유지")
    void givenAdjustedTradingHours_whenCreateWindow_thenPreservesExplicitTimes() {
        MinutePriceWindow window = MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "10:00", "20:00"), clock);

        assertThat(window.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(window.endTime()).isEqualTo(LocalTime.of(20, 0));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"J", "NX", "INVALID"})
    @DisplayName("[실패] - UN 외의 시장 또는 누락된 시장을 거부")
    void givenUnsupportedMarket_whenCreateWindow_thenRejectsRequest(String market) {
        assertThatThrownBy(() -> MinutePriceWindow.from(parameters(market, "INTEGRATED_DAY", "08:00", "20:00"), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"KRX_REGULAR", "NXT_MAIN"})
    @DisplayName("[실패] - 통합 일중 세션 외의 세션을 거부")
    void givenUnsupportedSession_whenCreateWindow_thenRejectsRequest(String session) {
        assertThatThrownBy(() -> MinutePriceWindow.from(parameters("UN", session, "08:00", "20:00"), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[실패] - 종료 시각보다 늦은 시작 시각을 거부")
    void givenReversedRange_whenCreateWindow_thenRejectsRequest() {
        assertThatThrownBy(() -> MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "20:00", "08:00"), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[실패] - 분 단위가 아닌 시각을 거부")
    void givenSecondsInRange_whenCreateWindow_thenRejectsRequest() {
        assertThatThrownBy(() -> MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "09:00:30", "20:00"), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[실패] - 수집 구간 종료 후 대기 시간이 지나지 않으면 거부")
    void givenUnsettledWindow_whenCreateWindow_thenRejectsRequest() {
        Clock beforeReady = Clock.fixed(Instant.parse("2026-11-12T11:09:59Z"), MinutePriceWindow.SEOUL);

        assertThatThrownBy(() -> MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "08:00", "20:00"), beforeReady))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[성공] - 수집 구간 종료 후 대기 시간이 지나면 허용")
    void givenSettledWindow_whenCreateWindow_thenAcceptsRequest() {
        Clock ready = Clock.fixed(Instant.parse("2026-11-12T11:10:00Z"), MinutePriceWindow.SEOUL);

        assertThat(MinutePriceWindow.from(parameters("UN", "INTEGRATED_DAY", "08:00", "20:00"), ready)).isNotNull();
    }

    private JobParameters parameters(String market, String session, String start, String end) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("tradeDate", "2026-11-12").addString("startTime", start).addString("endTime", end)
                .addString("collectionRevision", "0");

        if (market != null) {
            builder.addString("marketCode", market);
        }
        if (session != null) {
            builder.addString("session", session);
        }
        return builder.toJobParameters();
    }
}
