package bank.domain.decisions;

import bank.domain.facts.Amount;

/**
 * Pure function: calculates interest for a single account over a period of days.
 * Applies tax exemption logic — interest up to the yearly exemption is tax-free.
 * No I/O, no framework, no side effects.
 */
public final class InterestCalculationDecision {

    /**
     * Fixed-size interest rule parameters — extracted from the bank rules store.
     */
    public record InterestRules(
        double annualRatePercent,
        double taxRatePercent,
        double exemptionEuros
    ) {}

    /** Fixed-size business facts. */
    public record Facts(
        Amount balance,
        long days,
        Amount yearToDateInterest,
        InterestRules rules
    ) {}

    /** Complete computed result — all values needed for persistence and display. */
    public record Result(
        Amount grossInterest,
        Amount taxableInterest,
        Amount taxAmount,
        Amount netInterest
    ) {
        public static Result zero() {
            return new Result(Amount.ZERO, Amount.ZERO, Amount.ZERO, Amount.ZERO);
        }
    }

    /** Pure business computation. */
    public static Result decide(Facts facts) {
        if (facts.balance.compareTo(Amount.ZERO) <= 0) {
            return Result.zero();
        }
        if (facts.days <= 0) {
            return Result.zero();
        }

        // Gross interest = balance × annualRate% / 365 × days
        Amount grossInterest = facts.balance
            .times(facts.rules.annualRatePercent / 100.0 / 365.0 * facts.days);

        // Tax exemption: interest up to exemptionEuros per year is tax-free
        Amount exemption = Amount.ofEuros(facts.rules.exemptionEuros);
        Amount taxableInterest;
        if (facts.yearToDateInterest.compareTo(exemption) >= 0) {
            // Already exceeded exemption — all new interest is taxable
            taxableInterest = grossInterest;
        } else {
            Amount remaining = exemption.minus(facts.yearToDateInterest);
            if (grossInterest.compareTo(remaining) <= 0) {
                // All new interest fits within remaining exemption
                taxableInterest = Amount.ZERO;
            } else {
                // Only the excess is taxable
                taxableInterest = grossInterest.minus(remaining);
            }
        }

        Amount taxAmount = taxableInterest.times(facts.rules.taxRatePercent / 100.0);
        Amount netInterest = grossInterest.minus(taxAmount);

        return new Result(grossInterest, taxableInterest, taxAmount, netInterest);
    }
}
