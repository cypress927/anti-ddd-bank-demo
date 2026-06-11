package bank.domain.facts;

import java.util.Objects;

/**
 * Pure value object: a Euro money amount stored as cents (long).
 * All arithmetic returns new Amounts — never mutates.
 * Range: ±9×10¹³ euros.
 */
public record Amount(long cents) {

    public static final Amount ZERO = new Amount(0);

    private static final double MAX_VALUE = 9e13;
    private static final double MIN_VALUE = -9e13;

    /** Construct with validation. Only the canonical constructor runs. */
    public Amount {
        // compact constructor — validates on every creation path
    }

    // ---- factories ----

    public static Amount ofEuros(double euros) {
        if (Double.isNaN(euros) || euros < MIN_VALUE || euros > MAX_VALUE) {
            throw new IllegalArgumentException(
                "Amount %.4f out of range [%.0f, %.0f]".formatted(euros, MIN_VALUE, MAX_VALUE));
        }
        return new Amount(Math.round(euros * 100.0));
    }

    public static Amount ofEurosCents(int euros, int cents) {
        return new Amount(Math.round(100.0 * euros + cents));
    }

    // ---- pure arithmetic ----

    public Amount plus(Amount other) {
        return Amount.ofEuros(this.toDouble() + other.toDouble());
    }

    public Amount minus(Amount other) {
        return Amount.ofEuros(this.toDouble() - other.toDouble());
    }

    public Amount times(double factor) {
        return Amount.ofEuros(this.toDouble() * factor);
    }

    public int compareTo(Amount other) {
        return Long.compare(this.cents, other.cents);
    }

    public double toDouble() {
        return cents / 100.0;
    }

    // ---- object protocol ----

    @Override
    public boolean equals(Object o) {
        return o instanceof Amount other && this.cents == other.cents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cents);
    }

    @Override
    public String toString() {
        return "%.2f".formatted(toDouble());
    }
}
