package com.monow.api.portfolio.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.portfolio.dto.response.PortfolioHoldingResponse;
import com.monow.api.portfolio.dto.response.PortfolioResponse;
import com.monow.api.portfolio.dto.response.PortfolioSummaryResponse;
import com.monow.api.stock.application.currentprice.StockCurrentPriceMarketSelection;
import com.monow.api.stock.application.currentprice.StockCurrentPriceMarketSelector;
import com.monow.api.stock.application.realtime.StockRealtimePriceCacheService;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.holding.repository.PortfolioHoldingQueryResult;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioQueryService {

    private final AccountRepository accountRepository;

    private final HoldingRepository holdingRepository;

    private final StockRealtimePriceCacheService stockRealtimePriceCacheService;

    private final StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    public PortfolioResponse getPortfolio(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        List<PortfolioHoldingQueryResult> holdings = holdingRepository.findPortfolioHoldings(userId);

        StockCurrentPriceMarketSelection selection = stockCurrentPriceMarketSelector.select();

        List<String> stockCodes = holdings.stream()
                .map(PortfolioHoldingQueryResult::stockCode)
                .toList();

        Map<String, StockRealtimePriceResponse> currentPrices = stockRealtimePriceCacheService.findLatestPrices(selection.marketType(), stockCodes);

        List<PortfolioHoldingResponse> holdingResponses = new ArrayList<>();

        // 전체 투자 금액
        BigDecimal totalInvestment = BigDecimal.ZERO;
        // 전체 평가 금액
        BigDecimal stockEvaluationAmount = BigDecimal.ZERO;

        for (PortfolioHoldingQueryResult holding : holdings) {
            Integer quantity = holding.quantity();
            // 현재 종목의 총 매입 금액
            BigDecimal purchaseAmount = holding.totalPurchaseAmount();

            // 현재 종목의 현재가
            StockRealtimePriceResponse priceResponse = currentPrices.get(holding.stockCode());
            BigDecimal currentPrice = priceResponse.currentPrice();

            // 평균단가
            BigDecimal averageBuyPrice = purchaseAmount.divide(BigDecimal.valueOf(quantity), 2 ,RoundingMode.HALF_UP);

            // 종목 평가금액
            BigDecimal evaluationAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

            // 종목 평가 손익
            BigDecimal holdingProfitLoss = evaluationAmount.subtract(purchaseAmount);

            // 종목 수익률
            BigDecimal holdingProfitRate = calculateProfitRate(holdingProfitLoss, purchaseAmount);


            PortfolioHoldingResponse holdingResponse = new PortfolioHoldingResponse(
                    holding.stockCode(),
                    holding.stockName(),
                    quantity,
                    purchaseAmount,
                    averageBuyPrice,
                    currentPrice,
                    evaluationAmount,
                    holdingProfitLoss,
                    holdingProfitRate
            );

            holdingResponses.add(holdingResponse);

            totalInvestment = totalInvestment.add(purchaseAmount);
            stockEvaluationAmount = stockEvaluationAmount.add(evaluationAmount);
        }

        // 전체 평가 손익
        BigDecimal profitLoss = stockEvaluationAmount.subtract(totalInvestment);

        // 전체 수익률
        BigDecimal profitRate = calculateProfitRate(profitLoss, totalInvestment);

        // 현재 보유 현금
        BigDecimal cashBalance = account.getBalance();

        // 총 자산
        BigDecimal totalAsset = cashBalance.add(stockEvaluationAmount);

        PortfolioSummaryResponse summaryResponse = new PortfolioSummaryResponse(
                totalAsset,
                cashBalance,
                totalInvestment,
                stockEvaluationAmount,
                profitLoss,
                profitRate
        );

        return new PortfolioResponse(summaryResponse, holdingResponses);
    }


    private BigDecimal calculateProfitRate(
            BigDecimal profitLoss,
            BigDecimal investmentAmount
    ) {
        if (investmentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return profitLoss.multiply(BigDecimal.valueOf(100))
                .divide(investmentAmount, 2, RoundingMode.HALF_UP);
    }

}
