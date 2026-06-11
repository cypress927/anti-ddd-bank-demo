package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;

/**
 * Pure function: computes all fees for a transfer.
 * Produces a complete fee breakdown — orchestration never re-derives.
 * No I/O, no framework, no side effects.
 */
public final class TransferFeeDecision {

    /** Savings accounts get 6 free transfers per month before penalties. */
    private static final int SAVINGS_FREE_TRANSFERS = 6;
    /** Penalty per excess transfer for savings accounts (€). */
    private static final Amount SAVINGS_EXCESS_PENALTY = Amount.ofEuros(2.00);

    /**
     * Fixed-size fee rule parameters — extracted from the bank rules store.
     * Each field is a scalar; no collections, no entities.
     */
    public record FeeRules(
        double internalFeeEuros,
        double externalFlatFeeEuros,
        double externalPercentFee,
        double taxThresholdEuros,
        double taxRatePercent
    ) {}

    /** Fixed-size business facts for fee computation. */
    public record Facts(
        Amount amount,
        boolean isInternal,
        AccountType sourceAccountType,
        int monthlyTransferCount,
        FeeRules rules
    ) {}

    /** Complete computed result — all fee components, ready for display or persistence. */
    public record Result(
        Amount baseFee,
        Amount excessPenalty,
        Amount transactionTax,
        Amount totalFee
    ) {
        public static Result none() {
            return new Result(Amount.ZERO, Amount.ZERO, Amount.ZERO, Amount.ZERO);
        }
    }

    /** Pure business computation. */
    public static Result decide(Facts facts) {
        Amount baseFee;
        if (facts.isInternal) {
            baseFee = Amount.ofEuros(facts.rules.internalFeeEuros);
        } else {
            // External: flat fee + percentage of amount
            Amount flat = Amount.ofEuros(facts.rules.externalFlatFeeEuros);
            Amount percent = facts.amount.times(facts.rules.externalPercentFee / 100.0);
            Amount raw = flat.plus(percent);
            // Clamp: minimum 1.00€, maximum 50.00€
            Amount minFee = Amount.ofEuros(1.00);
            Amount maxFee = Amount.ofEuros(50.00);
            if (raw.compareTo(minFee) < 0) raw = minFee;
            if (raw.compareTo(maxFee) > 0) raw = maxFee;
            baseFee = raw;
        }

        // Excess transfer penalty (savings accounts only)
        Amount excessPenalty = Amount.ZERO;
        if (facts.sourceAccountType == AccountType.SAVINGS
                && facts.monthlyTransferCount >= SAVINGS_FREE_TRANSFERS) {
            excessPenalty = SAVINGS_EXCESS_PENALTY;
        }

        // Financial transaction tax: only on external transfers exceeding threshold
        Amount transactionTax = Amount.ZERO;
        if (!facts.isInternal) {
            Amount threshold = Amount.ofEuros(facts.rules.taxThresholdEuros);
            if (facts.amount.compareTo(threshold) > 0) {
                Amount excess = facts.amount.minus(threshold);
                transactionTax = excess.times(facts.rules.taxRatePercent / 100.0);
            }
        }

        Amount totalFee = baseFee.plus(excessPenalty).plus(transactionTax);
        return new Result(baseFee, excessPenalty, transactionTax, totalFee);
    }
}
