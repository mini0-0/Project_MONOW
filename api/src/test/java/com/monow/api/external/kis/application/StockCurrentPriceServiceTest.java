package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisCurrentPriceClient;
import com.monow.api.external.kis.dto.response.KisCurrentPriceResponse;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockCurrentPriceService;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class StockCurrentPriceServiceTest {

    @Mock
    private KisAccessTokenProvider kisAccessTokenProvider;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisCurrentPriceClient kisCurrentPriceClient;

    @InjectMocks
    private StockCurrentPriceService stockCurrentPriceService;


    @Nested
    @DisplayName("현재가 조회")
    class CurrentPrice {

        @Test
        @DisplayName("[성공] - 종목코드(stockCode)로 KRX 현재가를 조회하면 종목명과 현재가 정보를 반환")
        void currentPrice_whenValidStockCodeProvided_returnsCurrentPrice() {
            // Given
            String accessToken = "access_token";
            String stockCode = "005930";

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );


            KisCurrentPriceResponse.Output output = new KisCurrentPriceResponse.Output(
                    "005930",
                    "KOSPI200",
                    "전기·전자",
                    "322500",
                    "23500",
                    "2",
                    "7.86",
                    "31006148",
                    "10243164332536",
                    "326000",
                    "339000",
                    "320000"
            );

            KisCurrentPriceResponse kisResponse = new KisCurrentPriceResponse(
                    "0",
                     "MCA00000",
                     "정상처리 되었습니다.",
                     output
            );


            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));
            given(kisAccessTokenProvider.getAccessToken())
                    .willReturn(accessToken);
            given(kisCurrentPriceClient.fetchCurrentPrice(accessToken,stockCode, CurrentPriceMarketType.KRX))
                    .willReturn(kisResponse);

            // When
            StockCurrentPriceResponse response = stockCurrentPriceService.getCurrentPrice(stockCode, CurrentPriceMarketType.KRX);

            // Then
            assertThat(response.stockCode()).isEqualTo("005930");
            assertThat(response.stockName()).isEqualTo("삼성전자");
            assertThat(response.marketName()).isEqualTo("KOSPI200");
            assertThat(response.industryName()).isEqualTo("전기·전자");
            assertThat(response.currentPrice()).isEqualTo("322500");
            assertThat(response.changePrice()).isEqualTo("23500");
            assertThat(response.changeSign()).isEqualTo("2");
            assertThat(response.changeRate()).isEqualTo("7.86");
            assertThat(response.tradeVolume()).isEqualTo("31006148");
            assertThat(response.tradeAmount()).isEqualTo("10243164332536");
            assertThat(response.openPrice()).isEqualTo("326000");
            assertThat(response.highPrice()).isEqualTo("339000");
            assertThat(response.lowPrice()).isEqualTo("320000");
            assertThat(response.updatedAt()).isNotNull();

            log.info("현재가 조회 성공 response={}", response);

            verify(stockRepository).findByStockCode(stockCode);
            verify(kisAccessTokenProvider).getAccessToken();
            verify(kisCurrentPriceClient).fetchCurrentPrice(accessToken, stockCode, CurrentPriceMarketType.KRX);



        }


        @Test
        @DisplayName("[예외] - 존재하지 않는 종목코드면 현재가를 조회하지 않음")
        void currentPrice_whenStockDoesNotExist_throwsException() {
            // Given
            String stockCode = "000000";

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> stockCurrentPriceService.getCurrentPrice(
                            stockCode,
                            CurrentPriceMarketType.KRX)
            );

            verify(stockRepository).findByStockCode(stockCode);
            verify(kisAccessTokenProvider, never()).getAccessToken();
            verify(kisCurrentPriceClient, never())
                    .fetchCurrentPrice(anyString(), anyString(), any(CurrentPriceMarketType.class));
        }

        @Test
        @DisplayName("[예외] - KIS 현재가 응답 코드가 실패면 현재가를 반환하지 않음")
        void currentPrice_whenKisResponseFails_throwsException() {
            // Given
            String accessToken = "access_token";
            String stockCode = "005930";

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            KisCurrentPriceResponse kisResponse = new KisCurrentPriceResponse(
                    "1",
                    "KIS_CURRENT_PRICE_FETCH_FAILED",
                    "현재가 조회에 실패했습니다.",
                    null
            );

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(kisAccessTokenProvider.getAccessToken())
                    .willReturn(accessToken);
            given(kisCurrentPriceClient.fetchCurrentPrice(accessToken,stockCode,CurrentPriceMarketType.KRX))
                    .willReturn(kisResponse);


            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> stockCurrentPriceService.getCurrentPrice(
                            stockCode,
                            CurrentPriceMarketType.KRX
                    )
            );

            log.info("실패 예외 발생 errorCode={}", exception.getErrorCode());

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.KIS_CURRENT_PRICE_FETCH_FAILED);

            verify(stockRepository).findByStockCode(stockCode);
            verify(kisAccessTokenProvider).getAccessToken();
            verify(kisCurrentPriceClient).fetchCurrentPrice(
                    accessToken,
                    stockCode,
                    CurrentPriceMarketType.KRX
            );

        }

        @Test
        @DisplayName("[예외] - KIS 현재가 응답 output이 없으면 현재가를 반환하지 않음")
        void currentPrice_whenKisResponseOutputIsNull_throwsException() {
            // Given
            String accessToken = "access_token";
            String stockCode = "005930";

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            KisCurrentPriceResponse kisResponse = new KisCurrentPriceResponse(
                    "0",
                    "KIS_CURRENT_PRICE_INVALID_RESPONSE",
                    "현재가 조회에 실패했습니다.",
                    null
            );

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(kisAccessTokenProvider.getAccessToken())
                    .willReturn(accessToken);

            given(kisCurrentPriceClient.fetchCurrentPrice(
                    accessToken,
                    stockCode,
                    CurrentPriceMarketType.KRX
            )).willReturn(kisResponse);


            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> stockCurrentPriceService.getCurrentPrice(
                            stockCode,
                            CurrentPriceMarketType.KRX
                    )
            );

            log.info("실패 예외 발생 errorCode={}", exception.getErrorCode());

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.KIS_CURRENT_PRICE_INVALID_RESPONSE);

            verify(stockRepository).findByStockCode(stockCode);
            verify(kisAccessTokenProvider).getAccessToken();
            verify(kisCurrentPriceClient).fetchCurrentPrice(
                    accessToken,
                    stockCode,
                    CurrentPriceMarketType.KRX
            );
        }
    }
}
