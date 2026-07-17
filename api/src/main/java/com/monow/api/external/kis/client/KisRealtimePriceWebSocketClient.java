package com.monow.api.external.kis.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.request.KisRealtimePriceRequest;
import com.monow.api.external.kis.event.KisRealtimeSubscriptionEvent;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockRealtimePriceHandler;
import com.monow.api.external.kis.event.KisWebSocketConnectionEvent;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceWebSocketClient {

    private static final String REALTIME_PRICE_TR_ID = "H0STCNT0";

    private static final String PINGPONG_TR_ID = "PINGPONG";

    private static final String SUBSCRIBE_TR_TYPE = "1";

    private static final String CUSTOMER_TYPE_PERSONAL = "P";

    private static final String CONTENT_TYPE_UTF8 = "utf-8";

    private static final String REALTIME_DATA_TYPE = "0";

    private static final String SUCCESS_CODE = "0";

    private static final int MINIMUM_FIELD_COUNT = 15;

    private static final DateTimeFormatter KIS_TRADE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private static final DateTimeFormatter RESPONSE_TRADE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");


    private final KisProperties kisProperties;

    private final KisWebSocketApprovalKeyClient kisWebSocketApprovalKeyClient;

    private final StockRealtimePriceHandler stockRealtimePriceHandler;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper;

    private final Clock clock;
    private WebSocketSession session;

    private String approvalKey;

    /**
     * KIS WebSocket 서버에 연결하고, 연결된 WebSocketSession 저장
     * 주의:
     *      * connect() 메서드 종료가 실제 연결 완료를 의미하지 않음
     *      * 실제 연결 완료는 afterConnectionEstablished()에서 확인
     */
    public void connect() {
        approvalKey = kisWebSocketApprovalKeyClient.fetchApprovalKey();

        String websocketUrl = kisProperties.getWebsocketUrl();

        if (websocketUrl == null || websocketUrl.isBlank()) {
            throw new IllegalStateException("KIS WebSocket URL이 설정되어 있지 않습니다.");
        }

        WebSocketClient webSocketClient = new StandardWebSocketClient();

        TextWebSocketHandler handler = new TextWebSocketHandler() {

            /**
             * 실제 KIS WebSocket 연결 성공 시 실행
             */
            @Override
            public void afterConnectionEstablished(WebSocketSession webSocketSession) {
                KisRealtimePriceWebSocketClient.this.session = webSocketSession;

                log.info("KIS WebSocket 연결 성공 - sessionId={}", webSocketSession.getId());

                applicationEventPublisher.publishEvent(
                        new KisWebSocketConnectionEvent(
                                KisWebSocketConnectionEvent
                                        .ConnectionStatus
                                        .CONNECTED
                        )
                );
            }

            /**
             * KIS WebSocket 메시지 수신 시 실행
             */
            @Override
            protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) {
                String rawData = message.getPayload();

                log.info("KIS WebSocket 수신 데이터={}", rawData);

                try {
                    handleReceivedMessage(webSocketSession, rawData);
                } catch (Exception exception) {
                    log.error("KIS WebSocket 수신 데이터 처리 실패 - rawData={}", rawData, exception);
                }
            }

            /**
             * WebSocket 통신 오류 발생 시 실행
             */
            @Override
            public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) {
                log.error(
                        "KIS WebSocket 통신 오류 발생 - sessionId={}",
                        webSocketSession.getId(),
                        exception
                );
            }

            /**
             * 실제 KIS WebSocket 연결 종료 시 실행
             */
            @Override
            public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) {
                log.warn("KIS WebSocket 연결 종료 - sessionId={}, statusCode={}, reason={}",
                        webSocketSession.getId(),
                        status.getCode(),
                        status.getReason()
                );

                session = null;

                applicationEventPublisher.publishEvent(
                        new KisWebSocketConnectionEvent(
                                KisWebSocketConnectionEvent
                                        .ConnectionStatus
                                        .DISCONNECTED
                        )
                );
            }
        };


        /*
         * 비동기 WebSocket 연결 요청 시작
         */
        webSocketClient.execute(handler, websocketUrl);

        log.info("KIS WebSocket 연결 요청 완료 - websocketUrl={}", websocketUrl);
    }

    /**
     * 연결된 KIS WebSocket sesstion으로 특정 종목의 실시간 현재가 구독 요청 전송
     */
    public void subscribe(CurrentPriceMarketType marketType, String stockCode) {
        if (session == null || !session.isOpen()) {
            throw new BusinessException(ErrorCode.KIS_WEBSOCKET_NOT_CONNECTED);
        }

        if (approvalKey == null || approvalKey.isBlank()) {
            throw new BusinessException(ErrorCode.KIS_WEBSOCKET_APPROVAL_KEY_MISSING);
        }

        KisRealtimePriceRequest request = createRealtimePriceRequest(marketType, stockCode);

        String payload = createSubscribePayload(approvalKey, request);

        try {
            session.sendMessage(new TextMessage(payload));

            log.info("KIS 실시간 현재가 구독 요청 전송 - marketType={}, stockCode={}",
                    marketType,
                    stockCode
            );
        } catch (IOException exception) {
            log.error(
                    "KIS 실시간 현재가 구독 요청 실패 - marketType={}, stockCode={}",
                    marketType,
                    stockCode,
                    exception
            );
            throw new BusinessException(ErrorCode.KIS_WEBSOCKET_SUBSCRIBE_FAILED);
        }
    }

    /**
     * KIS WebSocket에서 받은 메시지 종류 구분
     * 실시간 체결 데이터
     */
    void handleReceivedMessage(WebSocketSession webSocketSession, String rawData) throws Exception {
        if (rawData == null || rawData.isBlank()) {
            throw invalidRealtimeDataException();
        }

        if (rawData.startsWith("{")) {
            handleJsonMessage(webSocketSession, rawData);
            return;
        }

        handleKisRealtimePriceData(rawData);

    }


    /**
     * KIS WebSocket에서 받은 JSON 메시지 처리
     *
     * JSON 메시지 종류:
     * 1. PINGPONG 연결 유지 메시지
     * 2. 종목 실시간 가격 수신 등록 결과
     */
    private void handleJsonMessage(WebSocketSession webSocketSession, String rawData) throws IOException {
        JsonNode rootNode = objectMapper.readTree(rawData);
        String trId = rootNode.path("header").path("tr_id").asText();

        if (PINGPONG_TR_ID.equals(trId)) {
            webSocketSession.sendMessage(new TextMessage(rawData));

            log.debug(
                    "KIS WebSocket PINGPONG 응답 전송 - sessionId={}",
                    webSocketSession.getId()
            );

            return;
        }

        if(!REALTIME_PRICE_TR_ID.equals(trId)) {
            log.debug("처리 대상이 아닌 KIS WebSocket JSON 메시지 - trId={}", trId);
            return;
        }

        handleSubscriptionResponse(rootNode);

    }

    /**
     * KIS 실시간 가격 수신 등록 성공·실패 응답 처리
     */
    private void handleSubscriptionResponse(JsonNode rootNode) {
        String stockCode = rootNode.path("header").path("tr_key").asText();
        String resultCode = rootNode.path("body").path("rt_cd").asText();
        String messageCode = rootNode.path("body").path("msg_cd").asText();
        String message = rootNode.path("body").path("msg1").asText();
        boolean success = SUCCESS_CODE.equals(resultCode);

        if (success) {
            log.info(
                    "KIS 실시간 현재가 등록 성공 - stockCode={}, messageCode={}, message={}",
                    stockCode,
                    messageCode,
                    message
            );
        } else {
            log.error(
                    "KIS 실시간 현재가 등록 실패 - stockCode={}, resultCode={}, messageCode={}, message={}",
                    stockCode,
                    resultCode,
                    messageCode,
                    message
            );
        }

        applicationEventPublisher.publishEvent(
                new KisRealtimeSubscriptionEvent(
                        CurrentPriceMarketType.KRX,
                        stockCode,
                        success,
                        message
                )
        );
    }

    /**
     * KIS WebSocket에 전송할 실시간 현재가 구독 요청 JSON 문자열 생성
     */
    public String createSubscribePayload(String approvalKey, KisRealtimePriceRequest request) {
        if (approvalKey == null || approvalKey.isBlank()) {
            throw new BusinessException(ErrorCode.KIS_WEBSOCKET_APPROVAL_KEY_MISSING);
        }

        if (request == null || request.stockCode() == null || request.stockCode().isBlank()) {
            throw new BusinessException(ErrorCode.KIS_WEBSOCKET_INVALID_SUBSCRIBE_REQUEST);
        }

        return """
                {
                  "header": {
                    "approval_key": "%s",
                    "custtype": "%s",
                    "tr_type": "%s",
                    "content-type": "%s"
                  },
                  "body": {
                    "input": {
                      "tr_id": "%s",
                      "tr_key": "%s"
                    }
                  }
                }
                """.formatted(
                approvalKey,
                CUSTOMER_TYPE_PERSONAL,
                SUBSCRIBE_TR_TYPE,
                CONTENT_TYPE_UTF8,
                REALTIME_PRICE_TR_ID,
                request.stockCode()
        );
    }

    /**
     * KIS에서 수신한 실시간 현재가 데이터를 파싱하고 내부 처리 Service로 전달
     */
    public void handleKisRealtimePriceData(String rawData) {
        if (rawData == null || rawData.isBlank()) {
            throw invalidRealtimeDataException();
        }

        String[] messageParts =
                rawData.split("\\|", 4);

        if (messageParts.length != 4) {
            throw invalidRealtimeDataException();
        }

        String encryptionFlag = messageParts[0];

        String trId = messageParts[1];

        String dataCountValue = messageParts[2];

        String body = messageParts[3];

        /*
         * 현재는 암호화되지 않은 데이터만 처리
         */
        if (!REALTIME_DATA_TYPE.equals(encryptionFlag)) {
            log.debug("암호화된 KIS WebSocket 데이터 처리 생략 - encryptionFlag={}", encryptionFlag);
            return;
        }

        /*
         * 국내주식 실시간 체결 데이터만 처리
         */
        if (!REALTIME_PRICE_TR_ID.equals(trId)) {
            log.debug("처리 대상이 아닌 KIS WebSocket 데이터 - trId={}", trId);
            return;
        }

        int dataCount;

        try {
            dataCount = Integer.parseInt(dataCountValue);
        } catch (NumberFormatException exception) {
            throw invalidRealtimeDataException();
        }

        /*
         * 현재 구현은 한 메시지에 체결 데이터 1건 처리
         */
        if (dataCount != 1) {
            log.warn("현재는 실시간 체결 데이터 1건만 처리합니다. dataCount={}", dataCount);

            return;
        }

        String[] fields = body.split("\\^", -1);

        if (fields.length < MINIMUM_FIELD_COUNT) {
            throw invalidRealtimeDataException();
        }

        RealtimeStockPriceResponse response = convertToRealtimeResponse(fields);

        /*
         * 실시간 현재가를 Redis에 저장하고 프론트 WebSocket topic으로 전달
         */
        stockRealtimePriceHandler.handleRealtimePrice(
                response.marketType(),
                response.stockCode(),
                response
        );

        log.info(
                "KIS 실시간 현재가 처리 완료 - stockCode={}, currentPrice={}, tradeTime={}",
                response.stockCode(),
                response.currentPrice(),
                response.tradeTime()
        );
    }
    private RealtimeStockPriceResponse convertToRealtimeResponse(String[] fields) {
        return new RealtimeStockPriceResponse(
                CurrentPriceMarketType.KRX,
                fields[0],
                fields[2],
                fields[4],
                fields[3],
                fields[5],
                fields[13],
                fields[14],
                fields[7],
                fields[8],
                fields[9],
                formatTradeTime(fields[1]),
                LocalDateTime.now(clock)
        );
    }

    /**
     * HHmmss를 HH:mm:ss로 변경
     */
    private String formatTradeTime(String rawTradeTime) {
        if (rawTradeTime == null || rawTradeTime.isBlank()) {
            throw invalidRealtimeDataException();
        }

        try {
            return LocalTime.parse(rawTradeTime, KIS_TRADE_TIME_FORMATTER)
                    .format(RESPONSE_TRADE_TIME_FORMATTER);
        } catch (Exception exception) {
            throw invalidRealtimeDataException();
        }
    }

    private IllegalArgumentException invalidRealtimeDataException() {
        return new IllegalArgumentException(
                "KIS 실시간 현재가 데이터가 올바른 데이터 형식이 아닙니다."
        );
    }

    /**
     * 시장 구분과 종목 코드로 KIS 실시간 현재가 요청 DTO를 생성
     */
    public KisRealtimePriceRequest createRealtimePriceRequest(CurrentPriceMarketType marketType, String stockCode) {
        String marketCode = marketType.getKisCode();

        return new KisRealtimePriceRequest(marketCode, stockCode);
    }
}
