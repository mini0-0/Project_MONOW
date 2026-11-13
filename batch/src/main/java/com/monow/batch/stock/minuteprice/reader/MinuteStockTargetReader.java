package com.monow.batch.stock.minuteprice.reader;

import com.monow.batch.stock.minuteprice.dto.MinuteStockTarget;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MinuteStockTargetReader implements ItemStreamReader<MinuteStockTarget> {
    private static final String TARGETS_KEY = "minutePrice.targets";
    private static final String INDEX_KEY = "minutePrice.index";
    private final JdbcTemplate jdbcTemplate;
    private List<MinuteStockTarget> targets;
    private String snapshot;
    private int index;

    public MinuteStockTargetReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void open(ExecutionContext context) {
        if (context.containsKey(TARGETS_KEY)) {
            snapshot = context.getString(TARGETS_KEY);
            targets = Arrays.stream(snapshot.split(",")).map(value -> {
                String[] fields = value.split(":", 2);
                return new MinuteStockTarget(Long.parseLong(fields[0]), fields[1]);
            }).toList();
        } else {
            targets = jdbcTemplate.query("""
                    SELECT id, stock_code FROM stocks
                    WHERE (krx_tradable = true OR nxt_tradable = true) AND is_active = true ORDER BY id
                    """, (rs, row) -> new MinuteStockTarget(rs.getLong("id"), rs.getString("stock_code")));
            if (targets.isEmpty()) {
                throw new IllegalStateException("통합 분봉 수집 대상 종목이 없습니다.");
            }
            if (targets.stream().anyMatch(target -> !target.stockCode().matches("[A-Za-z0-9]+"))) {
                throw new IllegalStateException("종목코드 형식이 잘못되었습니다.");
            }
            snapshot = targets.stream().map(target -> target.stockId() + ":" + target.stockCode())
                    .collect(Collectors.joining(","));
        }
        index = context.getInt(INDEX_KEY, 0);
        if (index < 0 || index > targets.size()) {
            throw new IllegalStateException("분봉 Reader 체크포인트가 잘못되었습니다.");
        }
    }

    @Override
    public MinuteStockTarget read() {
        return index < targets.size() ? targets.get(index++) : null;
    }

    @Override
    public void update(ExecutionContext context) {
        context.putString(TARGETS_KEY, snapshot);
        context.putInt(INDEX_KEY, index);
    }

    @Override
    public void close() {
        targets = null;
    }
}
