package bank.domain.decisions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeleteClientDecisionTest {

    @Test
    void allowWhenNoAccounts() {
        var result = DeleteClientDecision.decide(
            new DeleteClientDecision.Facts(false));
        assertTrue(result.allowed());
    }

    @Test
    void rejectWhenOwnsAccounts() {
        var result = DeleteClientDecision.decide(
            new DeleteClientDecision.Facts(true));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("owns"));
    }
}
