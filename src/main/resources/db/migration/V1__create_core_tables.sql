CREATE TABLE customer_account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_from VARCHAR(10) NOT NULL UNIQUE
    CHECK (account_from ~ '^[0-9]{10}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE budget_scope (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES customer_account(id),
    category VARCHAR(16) NOT NULL
        CHECK (category IN ('product', 'service')),
    CONSTRAINT uq_budget_scope_account_category
        UNIQUE (account_id, category)
);

CREATE TABLE spending_limit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_scope_id BIGINT NOT NULL REFERENCES budget_scope(id),
    amount_usd NUMERIC(19, 2) NOT NULL CHECK (amount_usd > 0),
    effective_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_spending_limit_scope_effective_at
        UNIQUE (budget_scope_id, effective_at)
);

CREATE TABLE monthly_spend (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_scope_id BIGINT NOT NULL REFERENCES budget_scope(id),
    month_start DATE NOT NULL,
    spent_usd NUMERIC(19, 2) NOT NULL DEFAULT 0
        CHECK (spent_usd >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_monthly_spend_scope_month
        UNIQUE (budget_scope_id, month_start)
);

CREATE TABLE currency_rate (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    base_currency CHAR(3) NOT NULL,
    quote_currency CHAR(3) NOT NULL CHECK (quote_currency = 'USD'),
    rate_date DATE NOT NULL,
    usd_per_unit NUMERIC(20, 12) NOT NULL
        CHECK (usd_per_unit > 0),
    provider VARCHAR(64) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_currency_rate_pair_date
        UNIQUE (base_currency, quote_currency, rate_date)
);

CREATE TABLE expense_transaction (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_scope_id BIGINT NOT NULL REFERENCES budget_scope(id),
    account_to VARCHAR(10) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    amount_original NUMERIC(19, 2) NOT NULL
        CHECK (amount_original > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processing_status VARCHAR(20) NOT NULL
        CHECK (processing_status IN (
            'PENDING_RATE',
            'ASSESSED',
            'RETRYABLE_ERROR'
        ))
);

CREATE TABLE transaction_assessment (
    transaction_id BIGINT PRIMARY KEY
        REFERENCES expense_transaction(id),
    amount_usd NUMERIC(19, 2) NOT NULL
        CHECK (amount_usd > 0),
    rate_id BIGINT REFERENCES currency_rate(id),
    monthly_spend_id BIGINT NOT NULL
        REFERENCES monthly_spend(id),
    limit_id BIGINT REFERENCES spending_limit(id),
    applied_limit_usd NUMERIC(19, 2) NOT NULL
        CHECK (applied_limit_usd > 0),
    limit_source VARCHAR(10) NOT NULL
        CHECK (limit_source IN ('DEFAULT', 'CUSTOM')),
    limit_exceeded BOOLEAN NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_assessment_limit_source
        CHECK (
            (limit_source = 'DEFAULT' AND limit_id IS NULL)
            OR
            (limit_source = 'CUSTOM' AND limit_id IS NOT NULL)
        )
);

CREATE INDEX idx_expense_transaction_scope_occurred_at_id
    ON expense_transaction (budget_scope_id, occurred_at, id);