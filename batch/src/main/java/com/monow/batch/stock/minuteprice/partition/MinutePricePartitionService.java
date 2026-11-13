package com.monow.batch.stock.minuteprice.partition;

import com.monow.batch.stock.minuteprice.dto.MinutePriceWindow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MinutePricePartitionService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("uuuuMM");
    private static final DateTimeFormatter BOUND_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    public MinutePricePartitionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void ensurePartition(YearMonth month) {
        String schema = parentSchema();
        jdbcTemplate.execute("SET LOCAL lock_timeout = '5s'");
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtext(current_database()), hashtext(?))",
                rs -> { }, "monow.minute.partition." + schema);
        String child = childName(month);
        Boolean exists = jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class,
                quote(schema) + "." + quote(child));
        if (!Boolean.TRUE.equals(exists)) {
            String from = month.atDay(1).atStartOfDay(MinutePriceWindow.SEOUL).toOffsetDateTime().toString();
            String to = month.plusMonths(1).atDay(1).atStartOfDay(MinutePriceWindow.SEOUL).toOffsetDateTime().toString();
            jdbcTemplate.execute("CREATE TABLE " + quote(schema) + "." + quote(child)
                    + " PARTITION OF " + quote(schema) + ".stock_price_minute"
                    + " FOR VALUES FROM ('" + from + "') TO ('" + to + "')");
        }
        requirePartition(month);
    }

    @Transactional
    public void requirePartition(YearMonth month) {
        String schema = parentSchema();
        // pg_get_expr의 출력 시간대를 고정해 실제 범위와 기대 범위를 비교한다.
        jdbcTemplate.execute("SET LOCAL TIME ZONE 'UTC'");
        List<String> bounds = jdbcTemplate.query("""
                SELECT pg_get_expr(child.relpartbound, child.oid)
                FROM pg_inherits inheritance
                JOIN pg_class child ON child.oid = inheritance.inhrelid
                JOIN pg_namespace namespace ON namespace.oid = child.relnamespace
                WHERE inheritance.inhparent = to_regclass('stock_price_minute')
                  AND namespace.nspname = ? AND child.relname = ?
                """, (rs, row) -> rs.getString(1), schema, childName(month));
        String expected = "FOR VALUES FROM ('" + utcBound(month) + "') TO ('" + utcBound(month.plusMonths(1)) + "')";
        if (bounds.size() != 1 || !expected.equals(bounds.getFirst())) {
            throw new IllegalStateException("분봉 월별 파티션이 없거나 범위가 잘못되었습니다: " + month);
        }
    }

    private String parentSchema() {
        List<String> schemas = jdbcTemplate.query("""
                SELECT namespace.nspname FROM pg_class parent
                JOIN pg_namespace namespace ON namespace.oid = parent.relnamespace
                WHERE parent.oid = to_regclass('stock_price_minute') AND parent.relkind = 'p'
                  AND pg_get_partkeydef(parent.oid) = 'RANGE (bar_at)'
                  AND EXISTS (
                    SELECT 1 FROM pg_constraint constraint_definition
                    WHERE constraint_definition.conrelid = parent.oid AND constraint_definition.contype = 'p'
                      AND pg_get_constraintdef(constraint_definition.oid)
                          = 'PRIMARY KEY (stock_id, market_code, bar_at)'
                  )
                """, (rs, row) -> rs.getString(1));
        if (schemas.size() != 1) {
            throw new IllegalStateException("분봉 부모 테이블 또는 복합 PK를 먼저 준비해야 합니다.");
        }
        return schemas.getFirst();
    }

    private String childName(YearMonth month) {
        if (month.getYear() < 1900 || month.getYear() > 9998) {
            throw new IllegalArgumentException("지원하지 않는 파티션 연도입니다.");
        }
        return "stock_price_minute_" + month.format(MONTH_FORMAT);
    }

    private String utcBound(YearMonth month) {
        return month.atDay(1).atStartOfDay(MinutePriceWindow.SEOUL).withZoneSameInstant(ZoneOffset.UTC)
                .format(BOUND_FORMAT) + "+00";
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
