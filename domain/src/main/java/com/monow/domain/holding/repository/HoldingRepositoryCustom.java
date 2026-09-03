package com.monow.domain.holding.repository;

import java.util.List;
public interface HoldingRepositoryCustom {

    List<PortfolioHoldingQueryResult> findPortfolioHoldings(Long userId);
}
