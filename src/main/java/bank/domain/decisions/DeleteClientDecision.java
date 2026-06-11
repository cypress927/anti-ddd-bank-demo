package bank.domain.decisions;

/**
 * Pure function: decides whether a client can be deleted.
 * No I/O, no framework, no side effects.
 */
public final class DeleteClientDecision {

    /** Fixed-size business fact: does the client own any accounts? */
    public record Facts(boolean ownsAnyAccount) {}

    public record Result(boolean allowed, String reason) {

        public static Result ok()                   { return new Result(true, null); }
        public static Result reject(String reason)  { return new Result(false, reason); }
    }

    /** The pure business computation. */
    public static Result decide(Facts facts) {
        if (facts.ownsAnyAccount) {
            return Result.reject(
                "Cannot delete client: still owns at least one account.");
        }
        return Result.ok();
    }
}
