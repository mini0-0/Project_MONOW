package com.monow.domain.transaction.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTransactionHistory is a Querydsl query type for TransactionHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTransactionHistory extends EntityPathBase<TransactionHistory> {

    private static final long serialVersionUID = -1499762066L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTransactionHistory transactionHistory = new QTransactionHistory("transactionHistory");

    public final com.monow.global.common.entity.QBaseTimeEntity _super = new com.monow.global.common.entity.QBaseTimeEntity(this);

    public final com.monow.domain.account.entity.QAccount account;

    public final NumberPath<java.math.BigDecimal> afterBalance = createNumber("afterBalance", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> beforeBalance = createNumber("beforeBalance", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.monow.domain.order.entity.QOrder order;

    public final EnumPath<TransactionHistoryType> transactionHistoryType = createEnum("transactionHistoryType", TransactionHistoryType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.monow.domain.user.entity.QUser user;

    public QTransactionHistory(String variable) {
        this(TransactionHistory.class, forVariable(variable), INITS);
    }

    public QTransactionHistory(Path<? extends TransactionHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTransactionHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTransactionHistory(PathMetadata metadata, PathInits inits) {
        this(TransactionHistory.class, metadata, inits);
    }

    public QTransactionHistory(Class<? extends TransactionHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new com.monow.domain.account.entity.QAccount(forProperty("account"), inits.get("account")) : null;
        this.order = inits.isInitialized("order") ? new com.monow.domain.order.entity.QOrder(forProperty("order"), inits.get("order")) : null;
        this.user = inits.isInitialized("user") ? new com.monow.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

