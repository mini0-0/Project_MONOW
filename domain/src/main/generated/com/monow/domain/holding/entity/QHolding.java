package com.monow.domain.holding.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QHolding is a Querydsl query type for Holding
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHolding extends EntityPathBase<Holding> {

    private static final long serialVersionUID = -2031372762L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QHolding holding = new QHolding("holding");

    public final com.monow.global.common.entity.QBaseTimeEntity _super = new com.monow.global.common.entity.QBaseTimeEntity(this);

    public final com.monow.domain.account.entity.QAccount account;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final com.monow.domain.stock.entity.QStock stock;

    public final NumberPath<java.math.BigDecimal> totalPurchaseAmount = createNumber("totalPurchaseAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.monow.domain.user.entity.QUser user;

    public QHolding(String variable) {
        this(Holding.class, forVariable(variable), INITS);
    }

    public QHolding(Path<? extends Holding> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QHolding(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QHolding(PathMetadata metadata, PathInits inits) {
        this(Holding.class, metadata, inits);
    }

    public QHolding(Class<? extends Holding> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new com.monow.domain.account.entity.QAccount(forProperty("account"), inits.get("account")) : null;
        this.stock = inits.isInitialized("stock") ? new com.monow.domain.stock.entity.QStock(forProperty("stock")) : null;
        this.user = inits.isInitialized("user") ? new com.monow.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

