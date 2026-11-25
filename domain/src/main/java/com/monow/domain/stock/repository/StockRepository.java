package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long>, StockRepositoryCustom {
    interface CorpCodeMappingTarget {
        String getStockCode();
        String getCorpCode();
    }

    @Query("select s.stockCode as stockCode, s.corpCode as corpCode from Stock s order by s.stockCode")
    List<CorpCodeMappingTarget> findCorpCodeMappingTargets();

    @Query("select distinct s.corpCode from Stock s where s.corpCode is not null order by s.corpCode")
    List<String> findDistinctCorpCodes();

    @Modifying
    @Query("""
            update Stock s set s.corpCode = :corpCode
            where s.stockCode = :stockCode
              and (s.corpCode = :previousCorpCode
                   or (:previousMissing = true and s.corpCode is null))
            """)
    int updateCorpCode(
            @Param("stockCode") String stockCode,
            @Param("corpCode") String corpCode,
            @Param("previousCorpCode") String previousCorpCode,
            @Param("previousMissing") boolean previousMissing
    );

    List<Stock> findByStockCodeIn(Collection<String> stockCodes);

    Optional<Stock> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    long countByStockCode(String stockCode);

    List<Stock> findByStockCodeInAndProductClassCodeNot(
            List<String> stockCodes,
            String productClassCode
    );

}
