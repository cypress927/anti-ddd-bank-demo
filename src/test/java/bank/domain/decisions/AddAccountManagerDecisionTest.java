package bank.domain.decisions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddAccountManagerDecisionTest {

    @Test
    void allowWhenOwnerAndNotYetManager() {
        var result = AddAccountManagerDecision.decide(
            new AddAccountManagerDecision.Facts(true, false));
        assertTrue(result.allowed());
    }

    @Test
    void rejectWhenNotOwner() {
        var result = AddAccountManagerDecision.decide(
            new AddAccountManagerDecision.Facts(false, false));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("owner"));
    }

    @Test
    void rejectWhenAlreadyManager() {
        var result = AddAccountManagerDecision.decide(
            new AddAccountManagerDecision.Facts(true, true));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("already"));
    }

    @Test
    void rejectWhenNotOwnerAndAlreadyManager() {
        // Both conditions fail — the first failure (not owner) wins
        var result = AddAccountManagerDecision.decide(
            new AddAccountManagerDecision.Facts(false, true));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("owner"));
    }
}
