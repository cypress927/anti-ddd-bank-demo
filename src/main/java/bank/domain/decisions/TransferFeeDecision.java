package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;

/**
 * Package-private pure function: computes transfer fees.
 * Called internally by {@link TransferDecision#decide} — orchestration
 * never calls this directly. Fee calculation is an implementation detail
 * of the transfer operation, not a standalone business unit.
 * No I/O, no framework, no side effects.
 */
final class TransferFeeDecision {

    /** Fixed-size business facts for fee computation — internal use only. */
    record Facts(
        Amount amount,
        boolean isInternal,
        AccountType sourceAccountType,
        int monthlyTransferCount,
        TransferDecision.FeeRules rules
    ) {}

    /** Complete computed fee result. */
    record Result(
        Amount baseFee,
        Amount excessPenalty,
        Amount transactionTax,
        Amount totalFee
    ) {}

    /** Pure business computation. */
    static Result decide(Facts facts) {
        Amount baseFee;
        if (facts.isInternal) {
            baseFee = Amount.ofEuros(facts.rules.internalFeeEuros());
        } else {
            Amount flat = Amount.ofEuros(facts.rules.externalFlatFeeEuros());
            Amount percent = facts.amount.times(facts.rules.externalPercentFee() / 100.0);
            Amount raw = flat.plus(percent);
            Amount minFee = Amount.ofEuros(facts.rules.externalMinFeeEuros());
            Amount maxFee = Amount.ofEuros(facts.rules.externalMaxFeeEuros());
            if (raw.compareTo(minFee) < 0) raw = minFee;
            if (raw.compareTo(maxFee) > 0) raw = maxFee;
            baseFee = raw;
        }

        Amount excessPenalty = Amount.ZERO;
        if (facts.sourceAccountType == AccountType.SAVINGS
                && facts.monthlyTransferCount >= facts.rules.savingsFreeTransfers()) {
            excessPenalty = Amount.ofEuros(facts.rules.savingsExcessPenaltyEuros());
        }

        Amount transactionTax = Amount.ZERO;
        if (!facts.isInternal) {
            Amount threshold = Amount.ofEuros(facts.rules.taxThresholdEuros());
            if (facts.amount.compareTo(threshold) > 0) {
                Amount excess = facts.amount.minus(threshold);
                transactionTax = excess.times(facts.rules.taxRatePercent() / 100.0);
            }
        }

        Amount totalFee = baseFee.plus(excessPenalty).plus(transactionTax);
        return new Result(baseFee, excessPenalty, transactionTax, totalFee);
    }
}
