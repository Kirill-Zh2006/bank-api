ALTER TABLE expense_transaction
    ADD COLUMN next_retry_at TIMESTAMPTZ;

UPDATE expense_transaction
SET next_retry_at = CURRENT_TIMESTAMP
WHERE processing_status = 'RETRYABLE_ERROR';