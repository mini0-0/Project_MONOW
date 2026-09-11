package com.monow.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockRepositoryCustom {

    Page<StockSearchQueryResult> searchByKeyword(String keyword, Pageable pageable);
}
