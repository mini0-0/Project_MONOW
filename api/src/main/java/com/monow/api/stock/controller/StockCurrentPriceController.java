package com.monow.api.stock.controller;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockCurrentPriceQueryService;
import com.monow.api.stock.application.StockRealtimePriceConnectionService;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.api.stock.dto.response.StockDetailResponse;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockCurrentPriceController {

    private static final String WEBSOCKET_ENDPOINT = "/ws";

    private final StockCurrentPriceQueryService stockCurrentPriceQueryService;

    private final StockRealtimePriceConnectionService stockRealtimePriceConnectionService;

    /**
     * 종목 상세 페이지 초기 현재가 조회
     *
     * 상세 페이지 진입 시 REST API로 현재가 상세 데이터 1회 조회
     * 실시간 변경 데이터는 WebSocket topic으로 갱신
     *
     * ex) GET /api/v1/stocks/005930/current-price/KRX
     */
    @GetMapping("/{stockCode}/current-price/{marketType}")
    public ApiResponse<StockCurrentPriceResponse> getCurrentPrice(
            @PathVariable(value = "stockCode") String stockCode,
            @PathVariable(value = "marketType") CurrentPriceMarketType marketType
    ) {
        StockCurrentPriceResponse response =
                stockCurrentPriceQueryService.getCurrentPrice(marketType, stockCode);

        return ApiResponse.success(response);
    }

    /**
     * 종목 상세 페이지 조회
     *
     * 상세 페이지 초기 현재가 데이터 조회
     * 실시간 데이터 수신용 WebSocket endpoint/topic 반환
     *
     * ex)
     * GET /api/v1/stocks/005930/detail?marketType=KRX
     */
    @GetMapping("/{stockCode}/detail")
    public ApiResponse<StockDetailResponse> getStockDetail(
            @PathVariable(value = "stockCode") String stockCode,
            @RequestParam(value = "marketType") CurrentPriceMarketType marketType
    ) {
        StockCurrentPriceResponse currentPrice =
                stockCurrentPriceQueryService.getCurrentPrice(marketType, stockCode);

        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        String realtimeTopic = createRealtimeTopic(marketType, stockCode);

        StockDetailResponse response = StockDetailResponse.from(
                currentPrice,
                WEBSOCKET_ENDPOINT,
                realtimeTopic
        );

        return ApiResponse.success(response);
    }


    /**
     * [개발 확인용] KIS WebSocket 연결 요청
     *
     * KIS WebSocket 서버 연결 확인용 API
     *
     * ex) POST /api/v1/stocks/realtime/connect
     */
    @PostMapping("/realtime/connect")
    public ApiResponse<String> connectRealtimePrice() {
        stockRealtimePriceConnectionService.connect();

        return ApiResponse.success("KIS 실시간 현재가 WebSocket 연결 요청 완료");
    }

    /**
     * [개발 확인용] 실시간 현재가 연결 등록
     *
     * 특정 종목의 실시간 현재가 연결 등록 요청 전송 확인용 API
     *
     * ex) POST /api/v1/stocks/005930/realtime/KRX/subscribe
     */
    @PostMapping("/{stockCode}/realtime/{marketType}/subscribe")
    public ApiResponse<String> subscribeRealtimePrice(
            @PathVariable(value = "stockCode") String stockCode,
            @PathVariable(value = "marketType") CurrentPriceMarketType marketType
    ) {
        stockRealtimePriceConnectionService.subscribe(marketType, stockCode);

        return ApiResponse.success(stockCode + " 실시간 현재가 연결 등록 요청 완료");
    }

    private String createRealtimeTopic(CurrentPriceMarketType marketType, String stockCode) {
        return "/topic/stocks/" + marketType.name() + "/" + stockCode;
    }
}