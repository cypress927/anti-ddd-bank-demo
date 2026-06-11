-- SQLite schema for Anti-DDD Bank Demo
-- Tables match the JPA entity definitions exactly.

CREATE TABLE IF NOT EXISTS clients (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT    NOT NULL UNIQUE,
    birth_date TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    account_no    INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    balance_cents INTEGER NOT NULL DEFAULT 0,
    account_type  TEXT    NOT NULL DEFAULT 'CHECKING'
);

CREATE TABLE IF NOT EXISTS account_accesses (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id  INTEGER NOT NULL REFERENCES clients(id),
    is_owner   INTEGER NOT NULL DEFAULT 0,
    account_no INTEGER NOT NULL REFERENCES accounts(account_no),
    UNIQUE(client_id, account_no)
);

-- Admin-configurable bank rules (key-value store)
CREATE TABLE IF NOT EXISTS bank_rules (
    rule_key   TEXT PRIMARY KEY,
    rule_value REAL NOT NULL,
    description TEXT
);

-- Transfer log for fee statistics and monthly transfer counting
CREATE TABLE IF NOT EXISTS transfer_logs (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    source_account_no    INTEGER NOT NULL,
    destination_account_no INTEGER,
    amount_cents         INTEGER NOT NULL,
    fee_cents            INTEGER NOT NULL DEFAULT 0,
    penalty_cents        INTEGER NOT NULL DEFAULT 0,
    tax_cents            INTEGER NOT NULL DEFAULT 0,
    is_internal          INTEGER NOT NULL DEFAULT 1,
    created_at           TEXT    NOT NULL
);

-- Interest accrual log for year-to-date tracking
CREATE TABLE IF NOT EXISTS interest_logs (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    account_no    INTEGER NOT NULL,
    balance_cents INTEGER NOT NULL,
    days          INTEGER NOT NULL,
    gross_cents   INTEGER NOT NULL DEFAULT 0,
    taxable_cents INTEGER NOT NULL DEFAULT 0,
    tax_cents     INTEGER NOT NULL DEFAULT 0,
    net_cents     INTEGER NOT NULL DEFAULT 0,
    year          INTEGER NOT NULL,
    created_at    TEXT    NOT NULL
);
