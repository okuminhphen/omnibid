DO $$
BEGIN
    IF to_regclass('public.wallet_transactions') IS NOT NULL THEN
        ALTER TABLE wallet_transactions
            DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;

        ALTER TABLE wallet_transactions
            ADD CONSTRAINT wallet_transactions_type_check
            CHECK (type IN ('FREEZE', 'REFUND', 'DEDUCT', 'TOP_UP', 'WITHDRAW'));
    END IF;
END
$$;
