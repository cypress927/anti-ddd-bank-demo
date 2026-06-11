package bank.domain.facts;

/**
 * Pure enumeration: the type of a bank account.
 * CHECKING — 活期账户 (no interest, overdraft allowed up to -1000€)
 * SAVINGS  — 储蓄账户 (earns interest, no overdraft, limited free transfers)
 */
public enum AccountType {
    CHECKING,
    SAVINGS
}
