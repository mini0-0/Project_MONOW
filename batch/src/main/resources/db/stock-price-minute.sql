CREATE TABLE stock_price_minute (
    stock_id BIGINT NOT NULL REFERENCES stocks(id),
    market_code VARCHAR(2) NOT NULL CHECK (market_code IN ('J', 'NX', 'UN')),
    bar_at TIMESTAMPTZ NOT NULL,
    open_price NUMERIC(18, 2) NOT NULL,
    high_price NUMERIC(18, 2) NOT NULL,
    low_price NUMERIC(18, 2) NOT NULL,
    close_price NUMERIC(18, 2) NOT NULL,
    volume BIGINT NOT NULL CHECK (volume >= 0),
    collected_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (stock_id, market_code, bar_at),
    CHECK (bar_at = date_trunc('minute', bar_at)),
    CHECK (low_price >= 0 AND high_price >= low_price
        AND open_price BETWEEN low_price AND high_price
        AND close_price BETWEEN low_price AND high_price)
) PARTITION BY RANGE (bar_at);
