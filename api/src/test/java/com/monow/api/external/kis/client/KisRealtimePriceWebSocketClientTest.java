package com.monow.api.external.kis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.request.KisRealtimePriceRequest;
import com.monow.api.external.kis.event.KisRealtimeSubscriptionEvent;
import com.monow.api.external.kis.mapper.KisRealtimePriceParser;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.realtime.StockRealtimePriceHandler;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisRealtimePriceWebSocketClientTest {

    @Mock
    private KisProperties kisProperties;

    @Mock
    private KisWebSocketApprovalKeyClient kisWebSocketApprovalKeyClient;

    @Mock
    private KisRealtimePriceParser kisRealtimePriceParser;

    @Mock
    private StockRealtimePriceHandler stockRealtimePriceHandler;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private KisRealtimePriceWebSocketClient kisRealtimePriceWebSocketClient;

    @BeforeEach
    void setUp() {
        kisRealtimePriceWebSocketClient = new KisRealtimePriceWebSocketClient(
                kisProperties,
                kisWebSocketApprovalKeyClient,
                kisRealtimePriceParser,
                stockRealtimePriceHandler,
                applicationEventPublisher,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("[성공] - 실시간 체결 메시지를 Parser로 변환한 뒤 Handler에 전달")
    void handleReceivedMessage_whenRealtimePriceReceived_passesParsedResponseToHandler() throws Exception {
        // Given
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String rawData = "0|H0STCNT0|001|005930^090015^310000^2^1000^0.32^309000^308000^311000^307000^0^0^0^100000^31000000000";

        StockRealtimePriceResponse response = createRealtimePriceResponse();

        given(kisRealtimePriceParser.parse(rawData))
                .willReturn(response);

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession, rawData);

        // Then
        verify(kisRealtimePriceParser).parse(rawData);

        verify(stockRealtimePriceHandler).handleRealtimePrice(
                CurrentPriceMarketType.KRX,
                "005930",
                response
        );
    }

    @Test
    @DisplayName("[성공] - Parser가 처리 대상이 아닌 데이터로 판단하면 Handler를 호출하지 않음")
    void handleReceivedMessage_whenParserReturnsNull_doesNotCallHandler() throws Exception {
        // Given
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String rawData = "1|H0STCNT0|001|encrypted-data";

        given(kisRealtimePriceParser.parse(rawData))
                .willReturn(null);

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession, rawData);

        // Then
        verify(kisRealtimePriceParser).parse(rawData);
        verify(stockRealtimePriceHandler, never()).handleRealtimePrice(any(), any(), any());
    }

    @Test
    @DisplayName("[성공] - KIS 구독 성공 응답이면 시장과 종목 정보를 포함한 이벤트 발행")
    void handleReceivedMessage_whenSubscriptionSucceeds_publishesSubscriptionEvent() throws Exception {
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
                    "msg_cd": "OPSP0000",
                    "msg1": "SUBSCRIBE SUCCESS"
                  }
                }
                """;

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession, rawData);

        // Then
        verify(kisRealtimePriceParser, never()).parse(any());

        verify(applicationEventPublisher).publishEvent(
                any(KisRealtimeSubscriptionEvent.class)
        );

        verify(stockRealtimePriceHandler, never()).handleRealtimePrice(any(), any(), any());
    }

    @Test
    @DisplayName("[성공] - marketType과 stockCode로 KIS 실시간 현재가 요청 생성")
    void createRealtimePriceRequest_whenValidInput_returnsRequest() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        // When
        KisRealtimePriceRequest request =
                kisRealtimePriceWebSocketClient.createRealtimePriceRequest(
                        marketType,
                        stockCode
                );

        // Then
        assert request.marketCode().equals(marketType.getKisCode());
        assert request.stockCode().equals(stockCode);
    }

    private StockRealtimePriceResponse createRealtimePriceResponse() {
        return new StockRealtimePriceResponse(
                CurrentPriceMarketType.KRX,
                "005930",
                BigDecimal.valueOf(310_000),
                BigDecimal.valueOf(1_000),
                "2",
                BigDecimal.valueOf(0.32),
                100_000L,
                BigDecimal.valueOf(31_000_000_000L),
                BigDecimal.valueOf(308_000),
                BigDecimal.valueOf(311_000),
                BigDecimal.valueOf(307_000),
                LocalTime.of(9, 0, 15),
                LocalDateTime.of(2026, 9, 2, 9, 0, 16)
        );
    }
}