package com.monow.batch.stock.master.writer;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DomesticStockMasterWriter implements ItemWriter<Stock> {

    private final StockRepository stockRepository;

    @Override
    public void write(Chunk<? extends Stock> chunk) {
        stockRepository.saveAll(chunk.getItems());

    }

}