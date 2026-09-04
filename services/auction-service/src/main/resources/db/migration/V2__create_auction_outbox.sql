CREATE TABLE IF NOT EXISTS auction_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP(6) WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    CONSTRAINT auction_outbox_event_type_check
        CHECK (event_type IN ('BID_PLACED', 'REFUND_REQUESTED')),
    CONSTRAINT auction_outbox_attempts_check CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_auction_outbox_pending
    ON auction_outbox_events (created_at)
    WHERE published_at IS NULL;
