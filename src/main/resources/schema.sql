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
    balance_cents INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS account_accesses (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id  INTEGER NOT NULL REFERENCES clients(id),
    is_owner   INTEGER NOT NULL DEFAULT 0,
    account_no INTEGER NOT NULL REFERENCES accounts(account_no),
    UNIQUE(client_id, account_no)
);
