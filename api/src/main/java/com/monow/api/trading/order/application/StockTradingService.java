package com.monow.api.trading.order.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.realtime.StockRealtimePriceCacheService;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.order.entity.Order;
import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.order.repository.OrderRepository;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.transaction.entity.TransactionHistory;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;
import com.monow.domain.user.entity.User;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockTradingService {

    private final AccountRepository accountRepository;

    private final StockRepository stockRepository;

    private final HoldingRepository holdingRepository;

    private final OrderRepository orderRepository;

    private final TransactionHistoryRepository transactionHistoryRepository;

    private final StockRealtimePriceCacheService stockRealtimePriceCacheService;

    @Transactional
    public void buyStock(
            Long userId,
            String stockCode,
            CurrentPriceMarketType marketType,
            int quantity
    ) {
        validateQuantity(quantity);

        Account account = getAccount(userId);
        User user = account.getUser();
        Stock stock = getStock(stockCode);

        BigDecimal executionPrice = getExecutionPrice(marketType, stockCode);

        OrderMarketType orderMarketType = convertOrderMarketType(marketType);

        BigDecimal totalAmount = executionPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal beforeBalance = account.getBalance();
        account.deductBalance(totalAmount);
        BigDecimal afterBalance = account.getBalance();

        Optional<Holding> existingHolding = holdingRepository.findByAccountAndStock(account, stock);

        if (existingHolding.isEmpty()) {
            Holding holding = Holding.createHolding(user, account, stock, quantity, totalAmount);

            holdingRepository.save(holding);
        } else {
            Holding holding = existingHolding.get();
            holding.buy(quantity, executionPrice);
        }


        LocalDateTime now = LocalDateTime.now();

        Order order = Order.createBuyOrder(user, account, stock, orderMarketType, quantity, executionPrice, totalAmount, now, now);
        orderRepository.save(order);

        String description = stock.getStockName() + " " + quantity + "주 매수";

        TransactionHistory transactionHistory = TransactionHistory.createBuyHistory(user, account, order, totalAmount, beforeBalance, afterBalance, description);
        transactionHistoryRepository.save(transactionHistory);
    }

    @Transactional
    public void sellStock(
            Long userId,
            String stockCode,
            CurrentPriceMarketType marketType,
            int quantity
    ) {
        validateQuantity(quantity);

        Account account = getAccount(userId);
        User user = account.getUser();
        Stock stock = getStock(stockCode);

        BigDecimal executionPrice = getExecutionPrice(marketType, stockCode);

        OrderMarketType orderMarketType = convertOrderMarketType(marketType);

        Holding holding = holdingRepository.findByAccountAndStock(account, stock)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));

        BigDecimal totalAmount = executionPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal beforeBalance = account.getBalance();

        holding.sell(quantity);
        account.addBalance(totalAmount);

        BigDecimal afterBalance = account.getBalance();

        LocalDateTime now = LocalDateTime.now();

        Order order = Order.createSellOrder(user, account, stock, orderMarketType, quantity, executionPrice, totalAmount, now, now);
        orderRepository.save(order);

        String description = stock.getStockName() + " " + quantity + "주 매도";

        TransactionHistory transactionHistory = TransactionHistory.createSellHistory(user, account, order, totalAmount, beforeBalance, afterBalance, description);
        transactionHistoryRepository.save(transactionHistory);
    }

    // 주문 수량 검증
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

    // 사용자 계좌 조회
    private Account getAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

    }

    // 종목 조회
    private Stock getStock(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

    }

    // 실시간 현재가 조회 및 검증
    private BigDecimal getExecutionPrice(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {
        StockRealtimePriceResponse response = stockRealtimePriceCacheService.findLatestPrice(marketType, stockCode);

        BigDecimal executionPrice = response.currentPrice();

        if (executionPrice == null || executionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REALTIME_PRICE_INVALID_RESPONSE);
        }

        return executionPrice;
    }

    private OrderMarketType convertOrderMarketType(
            CurrentPriceMarketType marketType
    ) {
        return switch (marketType) {
            case KRX -> OrderMarketType.KRX;
            case NXT -> OrderMarketType.NXT;
            case INTEGRATED -> OrderMarketType.INTEGRATED;
        };
    }

}
