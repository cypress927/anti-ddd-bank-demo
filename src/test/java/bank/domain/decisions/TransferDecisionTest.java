package bank.domain.decisions;

import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 * Each test: construct fixed-size Facts → call decide → assert plain Result.
 */
class TransferDecisionTest {

    private static final Amount DEST_BALANCE = Amount.ofEuros(300);

    @Test
    void allowValidTransfer() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100),  // amount
            true,                 // hasAccessRight
            Amount.ofEuros(500),  // source balance
            true,                 // destination exists
            DEST_BALANCE          // destination current balance
        );
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(400.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(400.0, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void rejectZeroAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ZERO, true, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("greater than 0"));
    }

    @Test
    void rejectNegativeAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(-10), true, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void rejectWhenNoAccessRight() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), false, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("access"));
    }

    @Test
    void rejectWhenBelowMinimumBalance() {
        // Source has 200 EUR, transfer 1500 EUR → new balance = -1300 < -1000 minimum
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(1500), true, Amount.ofEuros(200), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("minimum balance"));
    }

    @Test
    void allowExactlyAtMinimumBalance() {
        // Minimum balance is -1000 EUR. Transfer from 0 to reach exactly -1000.
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(1000), true, Amount.ZERO, true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(-1000.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(1300.0, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void rejectUnknownDestination() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, Amount.ofEuros(500), false, Amount.ZERO);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("does not exist"));
    }

    @Test
    void minimumBalanceConstantIsCorrect() {
        assertEquals(-1000.0, TransferDecision.MINIMUM_BALANCE.toDouble(), 0.001);
    }

    @Test
    void destinationBalanceIncreasesByTransferAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(250), true, Amount.ofEuros(1000),
            true, Amount.ofEuros(500));
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(750.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(750.0, result.newDestinationBalance().toDouble(), 0.001);
    }
}
