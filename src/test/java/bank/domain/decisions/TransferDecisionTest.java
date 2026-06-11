package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 */
class TransferDecisionTest {

    private static final Amount DEST_BALANCE = Amount.ofEuros(300);
    private static final Amount ZERO_FEE = Amount.ZERO;

    @Test
    void allowValidTransferChecking() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), ZERO_FEE, true,
            Amount.ofEuros(500), true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(400.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(400.0, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void rejectZeroAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ZERO, ZERO_FEE, true, Amount.ofEuros(500),
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("greater than 0"));
    }

    @Test
    void rejectNegativeAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(-10), ZERO_FEE, true, Amount.ofEuros(500),
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void rejectWhenNoAccessRight() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), ZERO_FEE, false, Amount.ofEuros(500),
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("access"));
    }

    @Test
    void rejectWhenBelowMinimumBalanceChecking() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(1500), ZERO_FEE, true, Amount.ofEuros(200),
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("minimum balance"));
    }

    @Test
    void allowExactlyAtMinimumBalanceChecking() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(1000), ZERO_FEE, true, Amount.ZERO,
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(-1000.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(1300.0, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void rejectUnknownDestination() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), ZERO_FEE, true, Amount.ofEuros(500),
            false, Amount.ZERO, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("does not exist"));
    }

    @Test
    void savingsCannotOverdraw() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), ZERO_FEE, true, Amount.ofEuros(50),
            true, DEST_BALANCE, AccountType.SAVINGS);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Savings accounts cannot overdraw"));
    }

    @Test
    void savingsCanTransferExactBalance() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(200), ZERO_FEE, true, Amount.ofEuros(200),
            true, DEST_BALANCE, AccountType.SAVINGS);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(0.0, result.newSourceBalance().toDouble(), 0.001);
    }

    @Test
    void feeIncreasesDeduction() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), Amount.ofEuros(5), true, Amount.ofEuros(200),
            true, DEST_BALANCE, AccountType.CHECKING);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        // Source: 200 - 100 - 5 = 95
        assertEquals(95.0, result.newSourceBalance().toDouble(), 0.001);
        // Dest only receives transfer amount: 300 + 100 = 400
        assertEquals(400.0, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void feeCausesRejectionInSavings() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), Amount.ofEuros(1), true, Amount.ofEuros(100),
            true, DEST_BALANCE, AccountType.SAVINGS);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("cannot overdraw"));
    }

    @Test
    void minimumBalanceConstantIsCorrect() {
        assertEquals(-1000.0, TransferDecision.CHECKING_MIN_BALANCE.toDouble(), 0.001);
    }
}
