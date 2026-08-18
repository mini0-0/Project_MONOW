package com.monow.domain.watchlist.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWatchlist is a Querydsl query type for Watchlist
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWatchlist extends EntityPathBase<Watchlist> {

    private static final long serialVersionUID = -1760059930L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWatchlist watchlist = new QWatchlist("watchlist");

    public final com.monow.global.common.entity.QBaseTimeEntity _super = new com.monow.global.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.monow.domain.stock.entity.QStock stock;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.monow.domain.user.entity.QUser user;

    public QWatchlist(String variable) {
        this(Watchlist.class, forVariable(variable), INITS);
    }

    public QWatchlist(Path<? extends Watchlist> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWatchlist(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWatchlist(PathMetadata metadata, PathInits inits) {
        this(Watchlist.class, metadata, inits);
    }

    public QWatchlist(Class<? extends Watchlist> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.stock = inits.isInitialized("stock") ? new com.monow.domain.stock.entity.QStock(forProperty("stock")) : null;
        this.user = inits.isInitialized("user") ? new com.monow.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

