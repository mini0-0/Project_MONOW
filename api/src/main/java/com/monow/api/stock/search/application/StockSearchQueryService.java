package com.monow.api.stock.search.application;

import com.monow.api.stock.search.dto.response.StockSearchResponse;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.stock.repository.StockSearchQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchQueryService {

    private final StockRepository stockRepository;

    public Page<StockSearchResponse> searchStocks(String keyword, Pageable pageable) {

        Page<StockSearchQueryResult> searchResults = stockRepository.searchByKeyword(keyword, pageable);

        Page<StockSearchResponse> responses = searchResults.map(StockSearchResponse::from);

        return responses;

    }
}
