package bank.domain.facts;

/**
 * Pure data record: fixed-size projection of an account-access link.
 * No behavior, no injected dependencies — just the facts.
 */
public record AccessFact(Long id, long clientId, String clientUsername,
                         boolean isOwner, AccountNo accountNo,
                         String accountName, Amount accountBalance) {

    public AccessFact {
        if (clientUsername == null || clientUsername.isBlank())
            throw new IllegalArgumentException("clientUsername is required");
        if (accountNo == null)
            throw new IllegalArgumentException("accountNo is required");
        if (accountBalance == null)
            accountBalance = Amount.ZERO;
    }

    public static AccessFact unsaved(long clientId, String clientUsername,
                                      boolean isOwner, AccountNo accountNo,
                                      String accountName, Amount accountBalance) {
        return new AccessFact(null, clientId, clientUsername,
            isOwner, accountNo, accountName, accountBalance);
    }

    public AccessFact withId(Long id) {
        return new AccessFact(id, this.clientId, this.clientUsername,
            this.isOwner, this.accountNo, this.accountName, this.accountBalance);
    }
}
