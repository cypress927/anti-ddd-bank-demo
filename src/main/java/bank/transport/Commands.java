package bank.transport;

/**
 * Plain data transfer objects — no behavior, just the shape of HTTP payloads.
 */
public final class Commands {

    public record CreateClientRequest(Long id, String username, String birthDate) {}
    public record CreateAccountRequest(String name) {}

    public record DepositRequest(Long accountNo, double amount) {}
    public record TransferRequest(Long sourceAccountNo, Long destinationAccountNo, double amount) {}
    public record AddAccountManagerRequest(Long accountNo, String username) {}

    // ---- response DTOs ----

    public record ClientResponse(Long id, String username, String birthDate) {
        public static ClientResponse from(bank.domain.facts.ClientFact f) {
            return new ClientResponse(f.id(), f.username(), f.birthDate().toString());
        }
    }

    public record AccountResponse(Long accountNo, String name, String balance) {
        public static AccountResponse from(bank.domain.facts.AccountFact f) {
            return new AccountResponse(f.accountNo().number(), f.name(), f.balance().toString());
        }
    }

    public record AccessResponse(String clientUsername, boolean isOwner,
                                  Long accountNo, String accountName, String accountBalance) {
        public static AccessResponse from(bank.domain.facts.AccessFact f) {
            return new AccessResponse(f.clientUsername(), f.isOwner(),
                f.accountNo().number(), f.accountName(), f.accountBalance().toString());
        }
    }
}
