package bank.domain.facts;

/**
 * Pure value object: typed wrapper around a positive long account number.
 */
public record AccountNo(long number) {

    public AccountNo {
        if (number < 0) {
            throw new IllegalArgumentException("Account number must be non-negative, got: " + number);
        }
    }

    public static AccountNo fromString(String s) {
        if (s == null || !s.matches("\\d+")) {
            throw new IllegalArgumentException("Illegal account number: " + s);
        }
        return new AccountNo(Long.parseLong(s));
    }

    @Override
    public String toString() {
        return Long.toString(number);
    }
}
