package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.global.config.QueryDSLConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDSLConfig.class)
class StockCorpCodeRepositoryTest {

    @Autowired
    private StockRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("[성공] - 같은 기업코드를 복수 종목에 저장하고 다른 컬럼은 유지한다")
    void givenCommonAndPreferredStocks_whenUpdateCorpCode_thenKeepBothStockRecords() {
        saveStock("005930", "삼성전자");
        saveStock("005935", "삼성전자우");

        assertThat(repository.updateCorpCode("005930", "00126380", null, true)).isEqualTo(1);
        assertThat(repository.updateCorpCode("005935", "00126380", null, true)).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findCorpCodeMappingTargets()).filteredOn(stock -> stock.getStockCode().startsWith("00593"))
                .extracting(StockRepository.CorpCodeMappingTarget::getCorpCode)
                .containsExactly("00126380", "00126380");
        assertThat(repository.findByStockCode("005930").orElseThrow().getStockName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("[실패] - 기존 값이 바뀌면 오래된 조회 결과로 덮어쓰지 않는다")
    void givenChangedCorpCode_whenUpdateWithStaleValue_thenKeepCurrentValue() {
        saveStock("000660", "SK하이닉스");
        assertThat(repository.updateCorpCode("000660", "00164779", null, true)).isEqualTo(1);

        assertThat(repository.updateCorpCode("000660", "00000001", null, true)).isZero();
        assertThat(repository.updateCorpCode("000660", "00000001", "00000002", false)).isZero();
        entityManager.clear();

        assertThat(repository.findByStockCode("000660").orElseThrow().getCorpCode()).isEqualTo("00164779");
    }

    private void saveStock(String stockCode, String stockName) {
        repository.saveAndFlush(Stock.createStock(null, null, stockCode, stockName, stockName,
                DomesticStockMarketType.KOSPI, null, null, null, null, null, true, false));
    }
}
