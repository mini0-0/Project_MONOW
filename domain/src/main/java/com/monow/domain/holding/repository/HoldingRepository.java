package com.monow.domain.holding.repository;

import com.monow.domain.account.entity.Account;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByAccountAndStock(Account account, Stock stock);

}
