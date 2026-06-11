package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;

/**
 * Pure function: decides whether a transfer is allowed and computes BOTH new balances.
 * Contains the minimum-balance business rule and account-type overdraft policy.
 * No I/O, no framework, no side effects.
 */
public final class TransferDecision {

    /** The minimum balance a checking account must maintain (allows overdraft up to 1000€). */
    public static final Amount CHECKING_MIN_BALANCE = Amount.ofEuros(-1000);

    /**
     * Fixed-size business facts. Every field is a scalar or flag — no entities, no collections.
     *
     * @param totalFee sum of all fees (base fee + penalty + tax), computed upstream by
     *        {@link TransferFeeDecision} — the pure function owns the deduction calculation
     * @param sourceAccountType determines overdraft policy (checking: -1000€, savings: no overdraft)
     * @param destinationCurrentBalance the destination's current balance, needed
     *        so the pure function can compute the destination result itself
     */
    public record Facts(
        Amount amount,
        Amount totalFee,
        boolean hasAccessRight,
        Amount sourceCurrentBalance,
        boolean destinationExists,
        Amount destinationCurrentBalance,
        AccountType sourceAccountType
    ) {}

    /**
     * Complete computed result: BOTH new balances, ready for persistence.
     * Orchestration does not need to derive any further business values.
     */
    public record Result(
        boolean allowed,
        String reason,
        Amount newSourceBalance,
        Amount newDestinationBalance
    ) {
        public static Result ok(Amount newSrc, Amount newDst) {
            return new Result(true, null, newSrc, newDst);
        }
        public static Result reject(String reason) {
            return new Result(false, reason, null, null);
        }
    }

    /**
     * Pure business computation — testable with plain data, no mocks.
     * Every value returned is complete: the caller only persists, it never re-derives.
     */
    public static Result decide(Facts facts) {
        // 1. Amount must be positive
        if (facts.amount.compareTo(Amount.ZERO) <= 0) {
            return Result.reject(
                "Transfer amount %s EUR must be greater than 0.".formatted(facts.amount));
        }
        // 2. Must have access right to the source account
        if (!facts.hasAccessRight) {
            return Result.reject(
                "You do not have access to the source account.");
        }
        // 3. Total deduction = amount + fee (source pays fees)
        Amount totalDeduction = facts.amount.plus(facts.totalFee);
        Amount newSourceBalance = facts.sourceCurrentBalance.minus(totalDeduction);
        // 4. Minimum balance check — depends on account type
        if (facts.sourceAccountType == AccountType.SAVINGS) {
            if (newSourceBalance.compareTo(Amount.ZERO) < 0) {
                return Result.reject(
                    "New balance %s EUR would be negative. Savings accounts cannot overdraw."
                        .formatted(newSourceBalance));
            }
        } else {
            if (newSourceBalance.compareTo(CHECKING_MIN_BALANCE) < 0) {
                return Result.reject(
                    "New balance %s EUR would fall below the minimum balance %s EUR."
                        .formatted(newSourceBalance, CHECKING_MIN_BALANCE));
            }
        }
        // 5. Destination must exist
        if (!facts.destinationExists) {
            return Result.reject("Destination account does not exist.");
        }
        // 6. Destination receives the transfer amount only (not fees)
        Amount newDestinationBalance = facts.destinationCurrentBalance.plus(facts.amount);
        return Result.ok(newSourceBalance, newDestinationBalance);
    }
}
