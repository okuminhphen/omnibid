CREATE TABLE IF NOT EXISTS auctions (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    starting_price NUMERIC(19, 2) NOT NULL,
    current_price NUMERIC(19, 2) NOT NULL,
    step_price NUMERIC(19, 2) NOT NULL,
    deposit_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    winning_user_id UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY,
    auction_id UUID NOT NULL,
    bidder_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    wallet_transaction_id UUID NOT NULL,
    placed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'auctions_status_check'
    ) THEN
        ALTER TABLE auctions
            ADD CONSTRAINT auctions_status_check
            CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'auctions_money_check'
    ) THEN
        ALTER TABLE auctions
            ADD CONSTRAINT auctions_money_check
            CHECK (
                starting_price >= 0
                AND current_price >= 0
                AND step_price > 0
                AND deposit_amount > 0
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'auctions_time_check'
    ) THEN
        ALTER TABLE auctions
            ADD CONSTRAINT auctions_time_check CHECK (end_time > start_time);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'bids_amount_check'
    ) THEN
        ALTER TABLE bids
            ADD CONSTRAINT bids_amount_check CHECK (amount > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_bids_auction'
    ) THEN
        ALTER TABLE bids
            ADD CONSTRAINT fk_bids_auction
            FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_bid_idempotency_key'
    ) THEN
        ALTER TABLE bids
            ADD CONSTRAINT uk_bid_idempotency_key UNIQUE (idempotency_key);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_auctions_status_end_time
    ON auctions (status, end_time);

CREATE INDEX IF NOT EXISTS idx_bids_auction_placed_at
    ON bids (auction_id, placed_at DESC);
