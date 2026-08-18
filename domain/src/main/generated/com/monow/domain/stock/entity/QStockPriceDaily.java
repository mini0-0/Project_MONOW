package com.monow.domain.stock.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStockPriceDaily is a Querydsl query type for StockPriceDaily
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStockPriceDaily extends EntityPathBase<StockPriceDaily> {

    private static final long serialVersionUID = 576222582L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStockPriceDaily stockPriceDaily = new QStockPriceDaily("stockPriceDaily");

    public final com.monow.global.common.entity.QBaseTimeEntity _super = new com.monow.global.common.entity.QBaseTimeEntity(this);

    public final NumberPath<java.math.BigDecimal> closePrice = createNumber("closePrice", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<java.math.BigDecimal> highPrice = createNumber("highPrice", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<java.math.BigDecimal> lowPrice = createNumber("lowPrice", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> openPrice = createNumber("openPrice", java.math.BigDecimal.class);

    public final QStock stock;

    public final DatePath<java.time.LocalDate> tradeDate = createDate("tradeDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> volume = createNumber("volume", Long.class);

    public QStockPriceDaily(String variable) {
        this(StockPriceDaily.class, forVariable(variable), INITS);
    }

    public QStockPriceDaily(Path<? extends StockPriceDaily> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStockPriceDaily(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStockPriceDaily(PathMetadata metadata, PathInits inits) {
        this(StockPriceDaily.class, metadata, inits);
    }

    public QStockPriceDaily(Class<? extends StockPriceDaily> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.stock = inits.isInitialized("stock") ? new QStock(forProperty("stock")) : null;
    }

}

