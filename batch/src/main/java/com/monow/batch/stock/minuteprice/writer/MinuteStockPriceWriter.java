package com.monow.batch.stock.minuteprice.writer;

import com.monow.domain.stock.model.StockPriceMinute;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;
import java.util.List;

public class MinuteStockPriceWriter implements ItemWriter<List<StockPriceMinute>> {
    private final JdbcBatchItemWriter<StockPriceMinute> delegate;

    public MinuteStockPriceWriter(DataSource dataSource) {
        delegate = new JdbcBatchItemWriterBuilder<StockPriceMinute>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO stock_price_minute AS existing (
                            stock_id, market_code, bar_at, open_price, high_price,
                            low_price, close_price, volume, collected_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (stock_id, market_code, bar_at) DO UPDATE SET
                            open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price,
                            low_price = EXCLUDED.low_price, close_price = EXCLUDED.close_price,
                            volume = EXCLUDED.volume, collected_at = EXCLUDED.collected_at
                        WHERE existing.collected_at <= EXCLUDED.collected_at
                          AND (existing.open_price, existing.high_price, existing.low_price,
                               existing.close_price, existing.volume)
                              IS DISTINCT FROM
                              (EXCLUDED.open_price, EXCLUDED.high_price, EXCLUDED.low_price,
                               EXCLUDED.close_price, EXCLUDED.volume)
                        """)
                .itemPreparedStatementSetter((price, statement) -> {
                    statement.setLong(1, price.stockId());
                    statement.setString(2, price.marketCode());
                    statement.setObject(3, price.barAt());
                    statement.setBigDecimal(4, price.openPrice());
                    statement.setBigDecimal(5, price.highPrice());
                    statement.setBigDecimal(6, price.lowPrice());
                    statement.setBigDecimal(7, price.closePrice());
                    statement.setLong(8, price.volume());
                    statement.setObject(9, price.collectedAt());
                })
                .assertUpdates(false)
                .build();
    }

    @Override
    public void write(Chunk<? extends List<StockPriceMinute>> chunk) throws Exception {
        delegate.write(new Chunk<>(chunk.getItems().stream().flatMap(List::stream).toList()));
    }
}
