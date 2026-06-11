package bank.domain.decisions;

import java.util.Map;

/**
 * Pure function: validates bank rule configuration changes.
 * Each rule has a name and a permitted numeric range.
 * No I/O, no framework, no side effects.
 */
public final class BankRuleConfigDecision {

    /** Valid ranges for each configurable rule — business constants. */
    private static final Map<String, double[]> RANGES = Map.of(
        "interest.rate",                new double[]{0, 20},
        "interest.tax.rate",            new double[]{0, 50},
        "interest.tax.exemption",       new double[]{0, 10_000},
        "transfer.fee.internal",        new double[]{0, 20},
        "transfer.fee.external.flat",   new double[]{0, 20},
        "transfer.fee.external.percent",new double[]{0, 5},
        "transfer.tax.threshold",       new double[]{0, 100_000},
        "transfer.tax.rate",            new double[]{0, 1}
    );

    /** Fixed-size business facts. */
    public record Facts(String ruleKey, double proposedValue) {}

    /** Complete business result. */
    public record Result(boolean allowed, String reason, double effectiveValue) {
        public static Result ok(double v)            { return new Result(true, null, v); }
        public static Result reject(String reason)   { return new Result(false, reason, 0); }
    }

    /** Pure business computation — validates the proposed value against the rule's range. */
    public static Result decide(Facts facts) {
        var range = RANGES.get(facts.ruleKey);
        if (range == null) {
            return Result.reject(
                "Unknown rule key \"%s\". Known rules: %s"
                    .formatted(facts.ruleKey, String.join(", ", RANGES.keySet())));
        }
        double min = range[0];
        double max = range[1];
        if (facts.proposedValue < min || facts.proposedValue > max) {
            return Result.reject(
                "Value %.4f for rule \"%s\" is out of range [%.0f, %.0f]."
                    .formatted(facts.proposedValue, facts.ruleKey, min, max));
        }
        return Result.ok(facts.proposedValue);
    }
}
