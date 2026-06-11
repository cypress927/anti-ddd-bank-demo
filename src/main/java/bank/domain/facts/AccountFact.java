package bank.domain.facts;

/**
 * Pure data record: fixed-size projection of an account.
 * No behavior, no injected dependencies — just the facts.
 */
public record AccountFact(AccountNo accountNo, String name, Amount balance, AccountType accountType) {

    public AccountFact {
        if (accountNo == null) throw new IllegalArgumentException("accountNo is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (balance == null) balance = Amount.ZERO;
        if (accountType == null) accountType = AccountType.CHECKING;
    }

    /** Before persistence, accountNo is not yet assigned. */
    public static AccountFact unsaved(String name, AccountType accountType) {
        return new AccountFact(null, name, Amount.ZERO, accountType);
    }

    public AccountFact withAccountNo(AccountNo no) {
        return new AccountFact(no, this.name, this.balance, this.accountType);
    }

    public AccountFact withBalance(Amount newBalance) {
        return new AccountFact(this.accountNo, this.name, newBalance, this.accountType);
    }
}
