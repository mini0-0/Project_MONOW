package com.monow.external.realtime.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.external.kis.config.KisProperties;
import com.monow.external.kis.realtime.client.KisRealtimePriceWebSocketClient;
import com.monow.external.kis.realtime.client.KisWebSocketApprovalKeyClient;
import com.monow.external.kis.realtime.dto.request.KisRealtimePriceRequest;
import com.monow.external.kis.realtime.event.KisRealtimePriceReceivedEvent;
import com.monow.external.kis.realtime.event.KisRealtimeSubscriptionEvent;
import com.monow.external.kis.realtime.model.KisRealtimePriceData;
import com.monow.external.kis.realtime.parser.KisRealtimePriceParser;
import com.monow.external.kis.type.CurrentPriceMarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    private ApplicationEventPublisher applicationEventPublisher;

    private KisRealtimePriceWebSocketClient kisRealtimePriceWebSocketClient;

    @BeforeEach
    void setUp() {
        kisRealtimePriceWebSocketClient = new KisRealtimePriceWebSocketClient(
                kisProperties,
                kisWebSocketApprovalKeyClient,
                kisRealtimePriceParser,
                applicationEventPublisher,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("[성공] - 실시간 체결 메시지를 파싱한 뒤 이벤트 발생")
    void handleReceivedMessage_whenRealtimePriceReceived_publishesRealtimePriceEvent() throws Exception {
        // Given
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String rawData = "0|H0STCNT0|001|005930^090015^310000^2^1000^0.32^309000^308000^311000^307000^0^0^0^100000^31000000000";

        KisRealtimePriceData data = createRealtimePriceData();

        given(kisRealtimePriceParser.parse(rawData))
                .willReturn(data);

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession, rawData);

        // Then
        verify(kisRealtimePriceParser).parse(rawData);

        ArgumentCaptor<KisRealtimePriceReceivedEvent> eventCaptor = ArgumentCaptor.forClass(KisRealtimePriceReceivedEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        KisRealtimePriceReceivedEvent event = eventCaptor.getValue();

        assertThat(event.data()).isEqualTo(data);

    }

    @Test
    @DisplayName("[성공] - Parser가 처리 대상이 아닌 데이터로 판단하면 수신 이벤트를 발행하지 않음")
    void  handleReceivedMessage_whenParserReturnsNull_doesNotPublishRealtimePriceEvent() throws Exception {
        // Given
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        String rawData = "1|H0STCNT0|001|encrypted-data";

        given(kisRealtimePriceParser.parse(rawData))
                .willReturn(null);

        // When
        kisRealtimePriceWebSocketClient.handleReceivedMessage(webSocketSession, rawData);

        // Then
        verify(kisRealtimePriceParser).parse(rawData);
        verify(applicationEventPublisher, never()).publishEvent(ArgumentMatchers.any(KisRealtimePriceReceivedEvent.class));

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
        verify(kisRealtimePriceParser, never()).parse(ArgumentMatchers.any());

        verify(applicationEventPublisher).publishEvent(
                ArgumentMatchers.any(KisRealtimeSubscriptionEvent.class)
        );

       verify(applicationEventPublisher, never()).publishEvent(ArgumentMatchers.any(KisRealtimePriceReceivedEvent.class));
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

    private KisRealtimePriceData createRealtimePriceData() {
        return new KisRealtimePriceData(
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