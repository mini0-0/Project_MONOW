package com.monow.domain.stock.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QStock is a Querydsl query type for Stock
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStock extends EntityPathBase<Stock> {

    private static final long serialVersionUID = 1605625478L;

    public static final QStock stock = new QStock("stock");

    public final com.monow.global.common.entity.QBaseTimeEntity _super = new com.monow.global.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath investmentProductTypeCode = createString("investmentProductTypeCode");

    public final StringPath investmentProductTypeName = createString("investmentProductTypeName");

    public final BooleanPath isActive = createBoolean("isActive");

    public final EnumPath<DomesticStockMarketType> marketType = createEnum("marketType", DomesticStockMarketType.class);

    public final StringPath productClassCode = createString("productClassCode");

    public final StringPath productClassName = createString("productClassName");

    public final StringPath productName = createString("productName");

    public final StringPath productNumber = createString("productNumber");

    public final StringPath productTypeCode = createString("productTypeCode");

    public final StringPath standardProductNumber = createString("standardProductNumber");

    public final StringPath stockCode = createString("stockCode");

    public final StringPath stockName = createString("stockName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QStock(String variable) {
        super(Stock.class, forVariable(variable));
    }

    public QStock(Path<? extends Stock> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStock(PathMetadata metadata) {
        super(Stock.class, metadata);
    }

}

