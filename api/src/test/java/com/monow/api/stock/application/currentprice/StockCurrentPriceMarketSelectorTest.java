package com.monow.api.stock.application.currentprice;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelection;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelector;
import com.monow.api.stock.currentprice.application.StockMarketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class StockCurrentPriceMarketSelectorTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("[성공] - 08시 30분이면 NXT 프리마켓 선택")
    void select_whenNxtPreMarketTime_returnsNxtPreMarket() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-01T23:30:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.NXT);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.PRE_MARKET);
        assertThat(result.realtime()).isTrue();
    }

    @Test
    @DisplayName("[성공] - 08시 55분이면 시장 전환 구간으로 실시간 조회 비활성화")
    void select_whenBetweenNxtAndKrx_returnsTransition() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-01T23:55:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.NXT);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.TRANSITION);
        assertThat(result.realtime()).isFalse();
    }

    @Test
    @DisplayName("[성공] - 09시 00분이면 KRX 정규시장 선택")
    void select_whenKrxOpeningTime_returnsKrx() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T00:00:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.KRX);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.OPEN);
        assertThat(result.realtime()).isTrue();
    }

    @Test
    @DisplayName("[성공] - 10시이면 KRX와 NXT 통합시장 선택")
    void select_whenIntegratedMarketTime_returnsIntegrated() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T01:00:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.INTEGRATED);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.OPEN);
        assertThat(result.realtime()).isTrue();
    }

    @Test
    @DisplayName("[성공] - 15시 25분이면 KRX 단독시장 선택")
    void select_whenAfterIntegratedMarket_returnsKrx() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T06:25:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.KRX);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.OPEN);
        assertThat(result.realtime()).isTrue();
    }

    @Test
    @DisplayName("[성공] - 15시 35분이면 시장 전환 구간으로 실시간 조회 비활성화")
    void select_whenBeforeNxtAfterMarket_returnsTransition() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T06:35:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.KRX);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.TRANSITION);
        assertThat(result.realtime()).isFalse();
    }

    @Test
    @DisplayName("[성공] - 16시이면 NXT 애프터마켓 선택")
    void select_whenNxtAfterMarketTime_returnsNxtAfterMarket() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T07:00:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.NXT);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.AFTER_MARKET);
        assertThat(result.realtime()).isTrue();
    }

    @Test
    @DisplayName("[성공] - 20시 이후이면 전체 시장 종료 상태 반환")
    void select_whenMarketClosed_returnsClosed() {
        // Given
        StockCurrentPriceMarketSelector selector =
                createSelector("2026-09-02T12:00:00Z");

        // When
        StockCurrentPriceMarketSelection result = selector.select();

        // Then
        assertThat(result.marketType()).isEqualTo(CurrentPriceMarketType.NXT);
        assertThat(result.marketStatus()).isEqualTo(StockMarketStatus.CLOSED);
        assertThat(result.realtime()).isFalse();
    }

    private StockCurrentPriceMarketSelector createSelector(String instant) {
        Clock clock = Clock.fixed(
                Instant.parse(instant),
                ZONE_ID
        );

        return new StockCurrentPriceMarketSelector(clock);
    }
}