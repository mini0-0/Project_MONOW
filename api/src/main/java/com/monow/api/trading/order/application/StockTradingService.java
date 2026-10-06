package com.monow.api.trading.order.application;

import com.monow.api.trading.order.dto.StockOrderPrice;
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
import com.monow.external.kis.type.CurrentPriceMarketType;
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

    private final StockOrderPriceService stockOrderPriceService;

    private final StockTradingTransactionService stockTradingTransactionService;
    public void buyStock(
            Long userId,
            String stockCode,
            int quantity
    ) {
        validateQuantity(quantity);


        // 주문 시점 현재가 조회
        StockOrderPrice orderPrice = stockOrderPriceService.getOrderPrice(stockCode);

        stockTradingTransactionService.buyStock(userId, stockCode, quantity, orderPrice);
    }

    public void sellStock(
            Long userId,
            String stockCode,
            int quantity
    ) {
        validateQuantity(quantity);

        // 주문 시점 현재가 조회
        StockOrderPrice orderPrice = stockOrderPriceService.getOrderPrice(stockCode);

        stockTradingTransactionService.sellStock(userId, stockCode, quantity, orderPrice);

    }

    // 주문 수량 검증
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

}
