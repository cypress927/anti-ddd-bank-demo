package bank.domain.decisions;

import bank.domain.facts.Amount;

/**
 * Pure function: decides whether a deposit is allowed and computes the new balance.
 * No I/O, no framework, no side effects.
 */
public final class DepositDecision {

    /** Fixed-size business facts. Note: oldBalance is NOT passed — repository loads it. */
    public record Facts(Amount amount, Amount currentBalance, boolean destinationExists) {}

    /** Complete computed result: allowed + new balance ready for persistence. */
    public record Result(boolean allowed, String reason, Amount newBalance) {

        public static Result ok(Amount newBalance)           { return new Result(true, null, newBalance); }
        public static Result reject(String reason)           { return new Result(false, reason, null); }
    }

    public static Result decide(Facts facts) {
        if (facts.amount.compareTo(Amount.ZERO) <= 0) {
            return Result.reject(
                "Deposit amount %s EUR must be greater than 0.".formatted(facts.amount));
        }
        if (!facts.destinationExists) {
            return Result.reject("Destination account does not exist.");
        }
        var newBalance = facts.currentBalance.plus(facts.amount);
        return Result.ok(newBalance);
    }
}
