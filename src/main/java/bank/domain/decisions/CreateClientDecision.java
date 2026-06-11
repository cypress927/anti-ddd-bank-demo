package bank.domain.decisions;

import bank.domain.facts.Amount;
import bank.domain.facts.AccountNo;

import java.util.regex.Pattern;

/**
 * Pure function: decides whether a new client can be created.
 * No I/O, no framework, no side effects.
 */
public final class CreateClientDecision {

    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("[a-z_A-Z][a-z_A-Z0-9]{0,30}");

    /** Fixed-size business facts required by this rule. */
    public record Facts(String username, boolean usernameAlreadyExists) {}

    /** Complete business result — enough for the next external action. */
    public record Result(boolean allowed, String reason) {

        public static Result ok()                     { return new Result(true, null); }
        public static Result reject(String reason)    { return new Result(false, reason); }
    }

    /** The pure business computation. */
    public static Result decide(Facts facts) {
        if (facts.username == null) {
            return Result.reject("Username must not be null.");
        }
        if (!USERNAME_PATTERN.matcher(facts.username).matches()) {
            return Result.reject(
                "Illegal username \"%s\". Must have 1–31 chars, start with a letter, "
                .formatted(facts.username)
                + "contain only english letters, underscores, and digits.");
        }
        if (facts.usernameAlreadyExists) {
            return Result.reject(
                "Username \"%s\" is already taken.".formatted(facts.username));
        }
        return Result.ok();
    }
}
