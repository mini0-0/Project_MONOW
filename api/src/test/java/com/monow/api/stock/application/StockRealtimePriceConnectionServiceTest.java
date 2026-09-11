package com.monow.api.stock.application;

import com.monow.api.external.kis.client.KisRealtimePriceWebSocketClient;
import com.monow.api.external.kis.event.KisWebSocketConnectionEvent;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.application.StockRealtimePriceConnectionService;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockRealtimePriceConnectionServiceTest {

    @Mock
    private KisRealtimePriceWebSocketClient kisRealtimePriceWebSocketClient;

    @InjectMocks
    private StockRealtimePriceConnectionService stockRealtimePriceConnectionService;

    @Test
    @DisplayName("[성공] - KIS 실시간 현재가 WebSocket 연결 요청을 Client에 위임")
    void connect_whenCalled_delegatesToKisRealtimePriceWebSocketClient() {
        // When
        stockRealtimePriceConnectionService.connect();

        // Then
        verify(kisRealtimePriceWebSocketClient).connect();


    }

    @Test
    @DisplayName("[성공] - 연결 전 종목 구독 요청 시 WebSocket 연결을 시작하고 실제 구독은 대기")
    void subscribe_whenDisconnected_connectsAndWaitsForConnection() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        // When
        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        // Then
        verify(kisRealtimePriceWebSocketClient).connect();
        verify(kisRealtimePriceWebSocketClient, never()).subscribe(marketType, stockCode);

    }

    @Test
    @DisplayName("[성공] - 연결 성공 이벤트 수신 시 대기 중인 삼성전자 종목을 실제 구독")
    void handleConnectionEvent_whenConnected_subscribesPendingStock() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        KisWebSocketConnectionEvent connectionEvent = new KisWebSocketConnectionEvent(
          KisWebSocketConnectionEvent.ConnectionStatus.CONNECTED
        );

        // When
        stockRealtimePriceConnectionService.handleConnectionEvent(connectionEvent);

        // Then
        verify(kisRealtimePriceWebSocketClient).subscribe(marketType, stockCode);
    }

    @Test
    @DisplayName("[성공] - 이미 연결된 상태에서 새로운 종목 요청 시 즉시 구독")
    void subscribe_whenConnected_subscribesImmediately() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String samsungStockCode = "005930";
        String skHynixStockCode = "000660";

        stockRealtimePriceConnectionService.subscribe(marketType, samsungStockCode);

        stockRealtimePriceConnectionService.onConnected();

        // When
        stockRealtimePriceConnectionService.subscribe(marketType, skHynixStockCode);

        // Then
        verify(kisRealtimePriceWebSocketClient).subscribe(marketType, samsungStockCode);
        verify(kisRealtimePriceWebSocketClient).subscribe(marketType, skHynixStockCode);

    }

    @Test
    @DisplayName("[성공] - 이미 구독한 종목을 다시 요청하면 중복 구독하지 않음")
    void subscribe_whenAlreadySubscribed_doesNotSubscribeAgain() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);
        stockRealtimePriceConnectionService.onConnected();

        // When
        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        // Then
        verify(kisRealtimePriceWebSocketClient, times(1)).subscribe(marketType, stockCode);
    }

    @Test
    @DisplayName("[성공] - 연결 종료 후 재연결되면 기존 구독 종목을 다시 구독")
    void handleConnectionEvent_whenReconnected_resubscribesPreviousStock() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);
        stockRealtimePriceConnectionService.onConnected();

        KisWebSocketConnectionEvent disconnectedEvent = new KisWebSocketConnectionEvent(
                KisWebSocketConnectionEvent.ConnectionStatus.DISCONNECTED
        );

        KisWebSocketConnectionEvent connectionEvent = new KisWebSocketConnectionEvent(
                KisWebSocketConnectionEvent.ConnectionStatus.CONNECTED
        );

        // When
        stockRealtimePriceConnectionService.handleConnectionEvent(disconnectedEvent);
        stockRealtimePriceConnectionService.handleConnectionEvent(connectionEvent);

        // Then
        verify(kisRealtimePriceWebSocketClient, times(2)).subscribe(marketType, stockCode);
    }


    @Test
    @DisplayName("[실패] - KIS WebSocket 연결 중 예외가 발생하면 예외를 그대로 전달")
    void connect_whenClientThrowsException_propagatesException() {
        // Given
        doThrow(new BusinessException(ErrorCode.KIS_WEBSOCKET_URL_MISSING))
                .when(kisRealtimePriceWebSocketClient)
                .connect();

        // When & Then
        assertThatThrownBy(() -> stockRealtimePriceConnectionService.connect())
                .isInstanceOf(BusinessException.class);

        verify(kisRealtimePriceWebSocketClient).connect();
    }

    @Test
    @DisplayName("[실패] - 연결 성공 후 실제 종목 구독 중 예외가 발생하면 예외를 그대로 전달")
    void subscribe_whenClientThrowsException_propagatesException() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        doThrow(new BusinessException(ErrorCode.KIS_WEBSOCKET_NOT_CONNECTED))
                .when(kisRealtimePriceWebSocketClient)
                .subscribe(marketType, stockCode);

        // When & Then
        assertThatThrownBy(() -> stockRealtimePriceConnectionService.onConnected())
                .isInstanceOf(BusinessException.class);

        verify(kisRealtimePriceWebSocketClient).subscribe(marketType, stockCode);
    }


}