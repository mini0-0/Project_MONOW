package com.monow.domain.stock.repository;

import java.util.List;

public interface StockRepositoryCustom {

    List<StockSearchQueryResult> searchByKeyword(String keyword);
}
