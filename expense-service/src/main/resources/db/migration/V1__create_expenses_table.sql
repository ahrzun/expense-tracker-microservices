-- Slice 2: initial expense storage.
--
-- Runs inside the expense_service schema (spring.flyway.default-schema), which
-- Flyway creates if absent. No other service migrates this schema.

CREATE TABLE expenses (
    id           UUID           PRIMARY KEY,

    description  VARCHAR(255)   NOT NULL,

    -- Money is NUMERIC, never float/double. Binary floating point cannot
    -- represent 0.10 exactly, so sums drift by cents over time. NUMERIC(12,2)
    -- is exact and caps a single expense below 10^10.
    amount       NUMERIC(12,2)  NOT NULL,

    category     VARCHAR(64)    NOT NULL,

    -- The calendar day the money was spent (a DATE, not a timestamp: "I spent
    -- this on the 3rd" has no meaningful time-of-day or time zone).
    spent_on     DATE           NOT NULL,

    -- Audit columns are TIMESTAMPTZ: these are instants, and instants need a
    -- time zone to be unambiguous across regions.
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,

    -- Enforced in the database as well as in Bean Validation. Application-layer
    -- validation protects the user experience; the constraint protects the data
    -- from every other writer, including a future service or a manual psql session.
    CONSTRAINT expenses_amount_positive CHECK (amount > 0)
);

-- Listing is "most recent first", which is the only read pattern this slice has.
CREATE INDEX idx_expenses_spent_on_desc ON expenses (spent_on DESC);
