package com.monow.batch.stock.master.processor;

import com.monow.external.kis.stock.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.stock.client.KisStockInfoClient;
import com.monow.batch.stock.master.dto.DomesticStockMasterItem;
import com.monow.external.kis.stock.dto.response.KisStockInfoResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DomesticStockMasterProcessorTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";
    private static final DomesticStockMarketType MARKET_TYPE = DomesticStockMarketType.KOSPI;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisAccessTokenProvider kisAccessTokenProvider;

    @Mock
    private KisStockInfoClient kisStockInfoClient;

    @Mock
    private KisStockInfoMapper kisStockInfoMapper;

    @InjectMocks
    private DomesticStockMasterProcessor domesticStockMasterProcessor;



    @Test
    @DisplayName("[성공] - 신규 종목이면 KIS 상세 정보를 조회해 Stock으로 변환")
    void process_whenNewStock_returnsStock() {
        // Given
        DomesticStockMasterItem masterItem = new DomesticStockMasterItem(
                STOCK_CODE,
                MARKET_TYPE,
                true,
                true
        );

        KisStockInfoResponse.Output output =
                new KisStockInfoResponse.Output(
                        "00000A005930",
                        "KR7005930003",
                        "005930",
                        "삼성전자보통주",
                        "삼성전자",
                        "300",
                        "101010",
                        "주권",
                        "1010",
                        "주식"
                );

        KisStockInfoResponse stockInfoResponse =
                new KisStockInfoResponse(
                        "0",
                        "MCA00000",
                        "정상처리 되었습니다.",
                        output
                );

        Stock expectedStock = createStock();

        given(stockRepository.existsByStockCode(STOCK_CODE))
                .willReturn(false);
        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);
        given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                .willReturn(stockInfoResponse);
        given(kisStockInfoMapper.toEntity(
                output,
                MARKET_TYPE,
                true,
                true
        )).willReturn(expectedStock);


        // When
        Stock result = domesticStockMasterProcessor.process(masterItem);

        // Then
        assertThat(result).isEqualTo(expectedStock);

        verify(stockRepository).existsByStockCode(STOCK_CODE);
        verify(kisAccessTokenProvider).getAccessToken();
        verify(kisStockInfoClient).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);


    }


    @Test
    @DisplayName("[예외] - 기존 종목 Batch 처리 제외")
    void process_whenStockAlreadyExists_returnsNull() {
        // Given
        DomesticStockMasterItem masterItem = new DomesticStockMasterItem(
                STOCK_CODE,
                MARKET_TYPE,
                true,
                true
        );

        given(stockRepository.existsByStockCode(STOCK_CODE))
                .willReturn(true);

        // When
        Stock result = domesticStockMasterProcessor.process(masterItem);

        // Then
        verify(stockRepository).existsByStockCode(STOCK_CODE);

    }

    private Stock createStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                STOCK_CODE,
                "삼성전자보통주",
                "삼성전자",
                DomesticStockMarketType.KOSPI,
                "300",
                "101010",
                "주권",
                "1010",
                "주식",
                true,
                true
        );
    }


}