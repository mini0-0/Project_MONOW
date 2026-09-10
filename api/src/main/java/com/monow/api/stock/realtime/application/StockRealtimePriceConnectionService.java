package com.monow.api.stock.realtime.application;

import com.monow.api.external.kis.client.KisRealtimePriceWebSocketClient;
import com.monow.api.external.kis.event.KisRealtimeSubscriptionEvent;
import com.monow.api.external.kis.event.KisWebSocketConnectionEvent;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelection;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRealtimePriceConnectionService {

    private static final long RECONNECT_DELAY_SECONDS = 3L;

    private final KisRealtimePriceWebSocketClient kisRealtimePriceWebSocketClient;

    private final StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    private final AtomicReference<ConnectionStatus> connectionStatus = new AtomicReference<>(ConnectionStatus.DISCONNECTED);

    private final Set<SubscriptionKey> pendingSubscriptions = ConcurrentHashMap.newKeySet();

    private final Set<SubscriptionKey> subscribedStocks = ConcurrentHashMap.newKeySet();

    private final Set<SubscriptionKey> requestingSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * WebSocket 연결 시작
     */
    public void connect() {
        boolean connectionStarted = connectionStatus.compareAndSet(
                ConnectionStatus.DISCONNECTED,
                ConnectionStatus.CONNECTING
        );

        if(!connectionStarted) {
            log.debug(
                    "KIS WebSocket 연결 요청 생략 - connectionStatus={}",
                    connectionStatus.get()
            );
            return;
        }

        try {
            kisRealtimePriceWebSocketClient.connect();

        } catch (RuntimeException exception) {
            connectionStatus.set(ConnectionStatus.DISCONNECTED);
            throw exception;
        }

    }

    /**
     * 현재 시간 기준으로 시장을 자동 선택하여 특정 종목 실시간 가격 수신 준비
     */
    public void subscribeCurrentMarket(String stockCode) {
        StockCurrentPriceMarketSelection selection = stockCurrentPriceMarketSelector.select();

        if (!selection.realtime()) {
            log.debug(
                    "실시간 가격 수신 등록 생략 - marketStatus={}, stockCode={}",
                    selection.marketStatus(),
                    stockCode
            );
            return;
        }

        subscribe(selection.marketType(), stockCode);
    }


    /**
     * 특정 종목의 실시간 가격 수신 준비
     */
    public void subscribe(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {
        SubscriptionKey subscriptionKey = new SubscriptionKey(marketType, stockCode);

        if (subscribedStocks.contains(subscriptionKey)) {
            log.debug(
                    "이미 실시간 가격 수신 중인 종목 - marketType={}, stockCode={}",
                    marketType,
                    stockCode
            );
            return;
        }

        if (requestingSubscriptions.contains(subscriptionKey)) {
            log.debug(
                    "이미 실시간 가격 등록 응답 대기 중인 종목 - marketType={}, stockCode={}",
                    marketType,
                    stockCode
            );
            return;
        }

        ConnectionStatus currentStatus = connectionStatus.get();

        if (currentStatus == ConnectionStatus.CONNECTED) {
            sendSubscriptionToKis(subscriptionKey);
            return;
        }

        pendingSubscriptions.add(subscriptionKey);
        connect();
    }

    /**
     * KisRealtimePriceWebSocketClient가 발행한
     * 실제 연결 성공 또는 종료 이벤트 수신
     */

    @EventListener
    public void handleConnectionEvent(
            KisWebSocketConnectionEvent event
    ) {
        if (event.status() == KisWebSocketConnectionEvent.ConnectionStatus.CONNECTED) {
            onConnected();
            return;
        }
        if (event.status() == KisWebSocketConnectionEvent.ConnectionStatus.DISCONNECTED) {
            onDisconnected();

        }

    }


    /**
     * KIS가 보낸 실제 종목 등록 성공·실패 응답 처리
     */
    @EventListener
    public void handleSubscriptionEvent(KisRealtimeSubscriptionEvent event) {
        SubscriptionKey subscriptionKey = new SubscriptionKey(
                event.marketType(),
                event.stockCode()
        );

        requestingSubscriptions.remove(subscriptionKey);

        if (event.success()) {
            subscribedStocks.add(subscriptionKey);
            pendingSubscriptions.remove(subscriptionKey);

            log.info(
                    "KIS 실시간 가격 수신 등록 확정 - marketType={}, stockCode={}",
                    event.marketType(),
                    event.stockCode()
            );

            return;
        }

        pendingSubscriptions.remove(subscriptionKey);

        log.error(
                "KIS 실시간 가격 수신 등록 실패 - marketType={}, stockCode={}, message={}",
                event.marketType(),
                event.stockCode(),
                event.message()
        );

    }

    /**
     * 실제 WebSocket 연결 성공 시 실행
     */
    public void onConnected() {
        connectionStatus.set(ConnectionStatus.CONNECTED);

        log.info("KIS WebSocket 연결 상태 변경 - connectionStatus=CONNECTED");

        Set<SubscriptionKey> copiedSubscriptions = Set.copyOf(pendingSubscriptions);

        /*
         * 대기 중인 종목을 하나씩 실제 구독 처리
         */
        for (SubscriptionKey subscriptionKey : copiedSubscriptions) {
            sendSubscriptionToKis(subscriptionKey);
        }
    }

    /**
     * 실제 WebSocket 연결 종료 시 실행
     */
    public void onDisconnected() {
        connectionStatus.set(ConnectionStatus.DISCONNECTED);

        pendingSubscriptions.addAll(subscribedStocks);
        pendingSubscriptions.addAll(requestingSubscriptions);

        subscribedStocks.clear();
        requestingSubscriptions.clear();

        log.warn(
                "KIS WebSocket 연결 상태 변경 - connectionStatus=DISCONNECTED, pendingCount={}",
                pendingSubscriptions.size()
        );

        if (!pendingSubscriptions.isEmpty()) {
            scheduleReconnect();
        }
    }

    /**
     * 연결 종료 후 일정 시간 뒤 재연결
     */
    private void scheduleReconnect() {
        log.info(
                "KIS WebSocket 재연결 예정. delaySeconds={}",
                RECONNECT_DELAY_SECONDS
        );

        CompletableFuture.delayedExecutor(
                RECONNECT_DELAY_SECONDS,
                TimeUnit.SECONDS
        ).execute(() -> {
            try {
                connect();
            } catch (RuntimeException exception) {
                log.error("KIS WebSocket 재연결 요청 실패", exception);

                connectionStatus.set(ConnectionStatus.DISCONNECTED);

                if (!pendingSubscriptions.isEmpty()) {
                    scheduleReconnect();
                }
            }
        });
    }


    /**
     * 실제 KIS WebSocket에 구독 메시지 전송
     */
    private void sendSubscriptionToKis(SubscriptionKey subscriptionKey) {
        if(subscribedStocks.contains(subscriptionKey)) {
            log.debug(
                    "이미 실시간 가격 수신 중인 종목 - marketType={}, stockCode={}",
                    subscriptionKey.marketType(),
                    subscriptionKey.stockCode()
            );
            return;
        }

        if (requestingSubscriptions.contains(subscriptionKey)) {
            log.debug(
                    "이미 실시간 가격 등록 응답 대기 중인 종목 - marketType={}, stockCode={}",
                    subscriptionKey.marketType(),
                    subscriptionKey.stockCode()
            );
            return;
        }

        kisRealtimePriceWebSocketClient.subscribe(
                subscriptionKey.marketType(),
                subscriptionKey.stockCode()
        );

        requestingSubscriptions.add(subscriptionKey);

        log.info(
                "KIS 실시간 종목 구독 요청 완료 - marketType={}, stockCode={}",
                subscriptionKey.marketType(),
                subscriptionKey.stockCode()
        );
    }

    private enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private record SubscriptionKey(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {
    }

}
