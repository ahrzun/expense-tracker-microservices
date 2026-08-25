-- Slice 3: user accounts for authentication.
--
-- Runs inside the auth_service schema. expense-service cannot see or migrate
-- this table, and this service cannot touch expense_service.expenses.

CREATE TABLE users (
    id             UUID          PRIMARY KEY,

    -- 320 is the maximum length of an RFC 5321 address (64 local + @ + 255 domain).
    -- Stored already lower-cased by the application, so this plain UNIQUE
    -- constraint is a genuine case-insensitive uniqueness guarantee rather than
    -- one that would let alice@x.com and Alice@x.com both register.
    email          VARCHAR(320)  NOT NULL,

    -- A BCrypt hash is 60 characters today. The column is wider on purpose:
    -- migrating to Argon2id later produces longer strings, and widening a
    -- column under load is a far more annoying migration than reserving space now.
    -- Named password_hash, never password — the plaintext is never stored,
    -- never logged, and never leaves the request thread.
    password_hash  VARCHAR(255)  NOT NULL,

    created_at     TIMESTAMPTZ   NOT NULL,

    CONSTRAINT users_email_unique UNIQUE (email)
);
