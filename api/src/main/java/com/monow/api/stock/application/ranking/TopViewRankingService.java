package com.monow.api.stock.application.ranking;


import com.monow.api.external.kis.client.KisTopViewClient;
import com.monow.api.external.kis.dto.response.KisTopViewItem;
import com.monow.api.stock.dto.response.TopViewRankingItemResponse;
import com.monow.api.stock.dto.response.TopViewRankingResponse;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopViewRankingService {

    private static final String RANKING_TYPE_TOP_VIEW = "TOP_VIEW";
    private static final String ETF_PRODUCT_CLASS_CODE = "101018";
    private static final int MAX_LIMIT = 10;

    private final KisTopViewClient kisTopViewClient;
    private final StockRepository stockRepository;


    public TopViewRankingResponse getTopViewRankStocks(int limit) {
        List<KisTopViewItem> kisItems = kisTopViewClient.fetchTopViewStocks();

        List<String> stockCodes = new ArrayList<>();

        // stockCode 추출
        for (KisTopViewItem kisItem : kisItems) {
            stockCodes.add(kisItem.stockCode());
        }

        List<Stock> stocks = stockRepository.findByStockCodeInAndProductClassCodeNot(
                stockCodes,
                ETF_PRODUCT_CLASS_CODE
        );


        Map<String, String> stockNameMap = new HashMap<>();

        for (Stock stock : stocks) {
            if (isPreferredStock(stock)) {
                continue;
            }

            stockNameMap.put(stock.getStockCode(), stock.getStockName());
        }

        List<TopViewRankingItemResponse> items = new ArrayList<>();

        for (KisTopViewItem kisItem : kisItems) {
            String stockCode = kisItem.stockCode();
            String stockName = stockNameMap.get(stockCode);

            if (stockName == null) {
                continue;
            }

            int rank = items.size() + 1;

            TopViewRankingItemResponse itemResponse = new TopViewRankingItemResponse(
                    rank,
                    kisItem.marketCode(),
                    stockCode,
                    stockName
            );

            items.add(itemResponse);

            if (items.size() == limit) {
                break;
            }
        }

        return new TopViewRankingResponse(RANKING_TYPE_TOP_VIEW, items);
    }
    private boolean isPreferredStock(Stock stock) {
        String stockName = stock.getStockName();

        return stockName.endsWith("우")
                || stockName.endsWith("우B");
    }

}
