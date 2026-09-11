package com.monow.api.external.kis.mapper;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceParser {

    private static final String REALTIME_DATA_TYPE = "0";

    private static final int MINIMUM_FIELD_COUNT = 15;

    private static final DateTimeFormatter KIS_TRADE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final Clock clock;

    /**
     * KIS에서 수신한 실시간 현재가 데이터를 파싱하고 내부 처리 Service로 전달
     */
    public StockRealtimePriceResponse parse(String rawData) {
        if (rawData == null || rawData.isBlank()) {
            throw invalidRealtimeDataException();
        }

        String[] messageParts = rawData.split("\\|", 4);

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
            return null;
        }

        /*
         * 국내주식 실시간 체결 데이터만 처리
         */
        if (!CurrentPriceMarketType.supportsRealtimeTrId(trId)) {
            log.debug("처리 대상이 아닌 KIS WebSocket 데이터 - trId={}", trId);
            return null;
        }

        CurrentPriceMarketType marketType = CurrentPriceMarketType.fromRealtimeTrId(trId);

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

            return null;
        }

        String[] fields = body.split("\\^", -1);

        if (fields.length < MINIMUM_FIELD_COUNT) {
            throw invalidRealtimeDataException();
        }

        return convertToRealtimeResponse(marketType, fields);
    }

    private StockRealtimePriceResponse convertToRealtimeResponse(CurrentPriceMarketType marketType, String[] fields) {
        try {
            return new StockRealtimePriceResponse(
                    marketType,
                    fields[0],
                    new BigDecimal(fields[2]),
                    new BigDecimal(fields[4]),
                    fields[3],
                    new BigDecimal(fields[5]),
                    Long.parseLong(fields[13]),
                    new BigDecimal(fields[14]),
                    new BigDecimal(fields[7]),
                    new BigDecimal(fields[8]),
                    new BigDecimal(fields[9]),
                    parseTradeTime(fields[1]),
                    LocalDateTime.now(clock)
            );
        } catch (NumberFormatException exception) {
            throw invalidRealtimeDataException();
        }
    }

    private LocalTime parseTradeTime(String rawTradeTime) {
        if (rawTradeTime == null || rawTradeTime.isBlank()) {
            throw invalidRealtimeDataException();
        }

        try {
            return LocalTime.parse(
                    rawTradeTime,
                    KIS_TRADE_TIME_FORMATTER
            );
        } catch (Exception exception) {
            throw invalidRealtimeDataException();
        }
    }


    private IllegalArgumentException invalidRealtimeDataException() {
        return new IllegalArgumentException(
                "KIS 실시간 현재가 데이터가 올바른 데이터 형식이 아닙니다."
        );
    }


}
