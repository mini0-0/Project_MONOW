package com.monow.api.external.kis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.request.KisRealtimePriceRequest;
import com.monow.api.external.kis.event.KisRealtimeSubscriptionEvent;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockRealtimePriceHandler;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisRealtimePriceWebSocketClientTest {

    @Mock
    private KisProperties kisProperties;

    @Mock
    private KisWebSocketApprovalKeyClient kisWebSocketApprovalKeyClient;

    @Mock
    private StockRealtimePriceHandler stockRealtimePriceHandler;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private KisRealtimePriceWebSocketClient kisRealtimePriceWebSocketClient;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-06T00:00:16Z"),
                ZoneId.of("Asia/Seoul")
        );
        objectMapper = new ObjectMapper();

        kisRealtimePriceWebSocketClient = new KisRealtimePriceWebSocketClient(
                kisProperties,
                kisWebSocketApprovalKeyClient,
                stockRealtimePriceHandler,
                applicationEventPublisher,
                objectMapper,
                fixedClock
        );
    }

    @Test
    @DisplayName("[성공] - KIS 실시간 현재가 데이터를 파싱해 Handler에 전달")
    void handleKisRealtimePriceData_whenValidData_passesToHandler() {
        // Given
        String rawData = "0|H0STCNT0|001|"
                + "005930^090015^310000^2^1000^0.32"
                + "^309000^308000^311000^307000"
                + "^0^0^0^100000^31000000000";

        // When
        kisRealtimePriceWebSocketClient.handleKisRealtimePriceData(rawData);

        // Then
        ArgumentCaptor<RealtimeStockPriceResponse> responseCaptor =
                ArgumentCaptor.forClass(RealtimeStockPriceResponse.class);

        verify(stockRealtimePriceHandler).handleRealtimePrice(
                eq(CurrentPriceMarketType.KRX),
                eq("005930"),
                responseCaptor.capture()
        );

        RealtimeStockPriceResponse response = responseCaptor.getValue();

        assertThat(response.marketType()).isEqualTo(CurrentPriceMarketType.KRX);
        assertThat(response.stockCode()).isEqualTo("005930");
        assertThat(response.currentPrice()).isEqualTo("310000");
        assertThat(response.changePrice()).isEqualTo("1000");
        assertThat(response.changeSign()).isEqualTo("2");
        assertThat(response.changeRate()).isEqualTo("0.32");
        assertThat(response.tradeVolume()).isEqualTo("100000");
        assertThat(response.tradeAmount()).isEqualTo("31000000000");
        assertThat(response.openPrice()).isEqualTo("308000");
        assertThat(response.highPrice()).isEqualTo("311000");
        assertThat(response.lowPrice()).isEqualTo("307000");
        assertThat(response.tradeTime()).isEqualTo("09:00:15");

        assertThat(response.updatedAt()).isEqualTo(
                LocalDateTime.of(2026, 6, 6, 9, 0, 16)
        );
    }

    @Test
    @DisplayName("[성공] - 시장 구분과 종목 코드로 KIS 구독 요청 데이터를 생성")
    void createRealtimePriceRequest_whenValidInput_returnsRequest() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";
        String expectedMarketCode = marketType.getKisCode();

        // When
        KisRealtimePriceRequest request =
                kisRealtimePriceWebSocketClient.createRealtimePriceRequest(
                        marketType,
                        stockCode
                );

        // Then
        assertThat(request.marketCode()).isEqualTo(expectedMarketCode);
        assertThat(request.stockCode()).isEqualTo(stockCode);
    }

    @Test
    @DisplayName("[실패] - KIS 실시간 현재가 데이터 형식이 올바르지 않으면 예외 발생")
    void handleKisRealtimePriceData_whenInvalidFormat_throwsException() {
        // Given
        String invalidRawData = "005930|310000|09:00:00";

        // When & Then
        assertThatThrownBy(
                () -> kisRealtimePriceWebSocketClient
                        .handleKisRealtimePriceData(invalidRawData)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "KIS 실시간 현재가 데이터가 올바른 데이터 형식이 아닙니다."
                );

        verify(stockRealtimePriceHandler, never()).handleRealtimePrice(
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("[성공] - KIS 구독 결과 JSON은 실시간 현재가로 처리하지 않음")
    void handleKisRealtimePriceData_whenSubscribeResponse_doesNotCallHandler() throws Exception{
        // Given
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String rawData = """
                {
                  "header": {
                    "tr_id": "H0STCNT0",
                    "tr_key": "005930"
                  },
                  "body": {
                    "rt_cd": "0",
                    "msg1": "SUBSCRIBE SUCCESS"
                  }
                }
                """;

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession,rawData);

        // Then
        verify(stockRealtimePriceHandler, never()).handleRealtimePrice(
                any(),
                any(),
                any()
        );
        verify(applicationEventPublisher).publishEvent(
                any(KisRealtimeSubscriptionEvent.class)
        );
    }
}