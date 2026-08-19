DO $$
BEGIN
    -- Existing Phase 1 databases have Hibernate's enum CHECK constraint,
    -- which ddl-auto=update does not evolve when TOP_UP is added to Java.
    IF to_regclass('public.wallet_transactions') IS NOT NULL THEN
        ALTER TABLE wallet_transactions
            DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;

        ALTER TABLE wallet_transactions
            ADD CONSTRAINT wallet_transactions_type_check
            CHECK (type IN ('FREEZE', 'REFUND', 'DEDUCT', 'TOP_UP'));
    END IF;
END
$$;
