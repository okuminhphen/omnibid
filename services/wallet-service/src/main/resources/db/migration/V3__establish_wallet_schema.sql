CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    frozen_balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    auction_id UUID,
    amount NUMERIC(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'wallets_balance_check'
    ) THEN
        ALTER TABLE wallets
            ADD CONSTRAINT wallets_balance_check
            CHECK (
                balance >= 0
                AND frozen_balance >= 0
                AND frozen_balance <= balance
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'wallet_transactions_amount_check'
    ) THEN
        ALTER TABLE wallet_transactions
            ADD CONSTRAINT wallet_transactions_amount_check CHECK (amount > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'wallet_transactions_status_check'
    ) THEN
        ALTER TABLE wallet_transactions
            ADD CONSTRAINT wallet_transactions_status_check
            CHECK (status IN ('SUCCESS', 'FAILED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'wallet_transactions_type_check'
    ) THEN
        ALTER TABLE wallet_transactions
            ADD CONSTRAINT wallet_transactions_type_check
            CHECK (type IN ('FREEZE', 'REFUND', 'DEDUCT', 'TOP_UP', 'WITHDRAW'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_wallet_transactions_wallet'
    ) THEN
        ALTER TABLE wallet_transactions
            ADD CONSTRAINT fk_wallet_transactions_wallet
            FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT;
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_user_id
    ON wallets (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_transaction_idempotency
    ON wallet_transactions (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_created_at
    ON wallet_transactions (wallet_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_auction
    ON wallet_transactions (auction_id)
    WHERE auction_id IS NOT NULL;
