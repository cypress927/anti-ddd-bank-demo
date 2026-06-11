package bank.domain.decisions;

/**
 * Pure function: decides whether a manager can be added to an account.
 * No I/O, no framework, no side effects.
 */
public final class AddAccountManagerDecision {

    /** Fixed-size business facts. */
    public record Facts(boolean requesterIsOwner, boolean managerAlreadyHasAccess) {}

    public record Result(boolean allowed, String reason) {

        public static Result ok()                   { return new Result(true, null); }
        public static Result reject(String reason)  { return new Result(false, reason); }
    }

    /** Pure business computation. */
    public static Result decide(Facts facts) {
        if (!facts.requesterIsOwner) {
            return Result.reject(
                "Only the account owner can add a manager.");
        }
        if (facts.managerAlreadyHasAccess) {
            return Result.reject(
                "The manager already has access to this account.");
        }
        return Result.ok();
    }
}
