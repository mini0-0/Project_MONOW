package com.monow.batch.stock.dailyprice.writer;

import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyStockPriceWriter implements ItemWriter<List<StockPriceDaily>> {

    private final StockPriceDailyRepository stockPriceDailyRepository;

    @Override
    public void write(Chunk<? extends List<StockPriceDaily>> chunk) {
        List<StockPriceDaily> dailyPrices = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();

        stockPriceDailyRepository.saveAll(dailyPrices);
    }

}
