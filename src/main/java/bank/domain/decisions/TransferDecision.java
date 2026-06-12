package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;

/**
 * Pure function: the single entry point for a complete transfer operation.
 * Internally composes fee calculation and balance rules — orchestration
 * calls once and receives everything needed to persist and display.
 * No I/O, no framework, no side effects.
 */
public final class TransferDecision {

    /** The minimum balance a checking account must maintain (allows overdraft up to 1000�?). */
    public static final Amount CHECKING_MIN_BALANCE = Amount.ofEuros(-1000);

    /**
     * Fixed-size fee rule parameters — extracted from the bank rules store.
     * Each field is a scalar; no collections, no entities.
     */
    public record FeeRules(
        double internalFeeEuros,
        double externalFlatFeeEuros,
        double externalPercentFee,
        double externalMinFeeEuros,
        double externalMaxFeeEuros,
        double taxThresholdEuros,
        double taxRatePercent,
        int savingsFreeTransfers,
        double savingsExcessPenaltyEuros
    ) {}

    /**
     * Fixed-size business facts — all raw inputs for a transfer.
     * No pre-computed values: fees are computed internally by this function.
     */
    public record Facts(
        Amount amount,
        boolean isInternal,
        AccountType sourceAccountType,
        int monthlyTransferCount,
        FeeRules feeRules,
        boolean hasAccessRight,
        Amount sourceCurrentBalance,
        boolean destinationExists,
        Amount destinationCurrentBalance
    ) {}

    /**
     * Complete computed result — decision + both new balances + full fee breakdown.
     * Orchestration persists and displays, never re-derives.
     */
    public record Result(
        boolean allowed,
        String reason,
        Amount newSourceBalance,
        Amount newDestinationBalance,
        Amount baseFee,
        Amount excessPenalty,
        Amount transactionTax,
        Amount totalFee
    ) {
        public static Result ok(Amount newSrc, Amount newDst,
                                 Amount baseFee, Amount penalty, Amount tax, Amount totalFee) {
            return new Result(true, null, newSrc, newDst, baseFee, penalty, tax, totalFee);
        }
        public static Result reject(String reason) {
            return new Result(false, reason, null, null, null, null, null, null);
        }
    }

    /**
     * Pure business computation — one call for the entire transfer.
     * Fees are computed internally; orchestration never sees the sequence.
     */
    public static Result decide(Facts facts) {
        // 1. Compute fees internally — orchestration doesn't know this step exists
        var feeResult = TransferFeeDecision.decide(
            new TransferFeeDecision.Facts(facts.amount, facts.isInternal,
                facts.sourceAccountType, facts.monthlyTransferCount, facts.feeRules));

        Amount totalFee = feeResult.totalFee();

        // 2. Amount must be positive
        if (facts.amount.compareTo(Amount.ZERO) <= 0) {
            return Result.reject(
                "Transfer amount %s EUR must be greater than 0.".formatted(facts.amount));
        }
        // 3. Must have access right to the source account
        if (!facts.hasAccessRight) {
            return Result.reject(
                "You do not have access to the source account.");
        }
        // 4. Total deduction = amount + fee (source pays fees)
        Amount totalDeduction = facts.amount.plus(totalFee);
        Amount newSourceBalance = facts.sourceCurrentBalance.minus(totalDeduction);
        // 5. Minimum balance check — depends on account type
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
        // 6. Destination must exist
        if (!facts.destinationExists) {
            return Result.reject("Destination account does not exist.");
        }
        // 7. Destination receives the transfer amount only (not fees)
        Amount newDestinationBalance = facts.destinationCurrentBalance.plus(facts.amount);

        return Result.ok(newSourceBalance, newDestinationBalance,
            feeResult.baseFee(), feeResult.excessPenalty(),
            feeResult.transactionTax(), totalFee);
    }
}
