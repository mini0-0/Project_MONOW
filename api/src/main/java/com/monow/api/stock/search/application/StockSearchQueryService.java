package com.monow.api.stock.search.application;

import com.monow.api.stock.search.dto.response.StockSearchResponse;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.stock.repository.StockSearchQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchQueryService {

    private final StockRepository stockRepository;

    public List<StockSearchResponse> searchStocks(String keyword) {

        List<StockSearchQueryResult> searchResults = stockRepository.searchByKeyword(keyword);

        List<StockSearchResponse> responses = searchResults.stream()
                                                .map(StockSearchResponse::from)
                                                .toList();

        return responses;

    }
}
