package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 */
class TransferDecisionTest {

    private static final TransferDecision.FeeRules DEFAULT_RULES =
        new TransferDecision.FeeRules(0.50, 1.00, 0.05, 1.00, 50.00, 10_000.0, 0.1, 6, 2.00);
    private static final Amount DEST_BALANCE = Amount.ofEuros(300);

    @Test
    void allowValidTransferChecking() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        // Source: 500 - 100 - 0.50(fee) = 399.50
        assertEquals(399.50, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(400.0, result.newDestinationBalance().toDouble(), 0.001);
        assertEquals(0.50, result.totalFee().toDouble(), 0.001);
    }

    @Test
    void rejectZeroAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ZERO, true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("greater than 0"));
    }

    @Test
    void rejectNegativeAmount() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(-10), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void rejectWhenNoAccessRight() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            false, Amount.ofEuros(500), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("access"));
    }

    @Test
    void rejectWhenBelowMinimumBalanceChecking() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(1500), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(200), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("minimum balance"));
    }

    @Test
    void allowExactlyAtMinimumBalanceChecking() {
        // Source=0, transfer=999.50 → deduction=999.50+0.50=1000 → newBalance=-1000 (exactly at min)
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(999.50), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ZERO, true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(-1000.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(1299.50, result.newDestinationBalance().toDouble(), 0.001);
    }

    @Test
    void rejectUnknownDestination() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(500), false, Amount.ZERO);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("does not exist"));
    }

    @Test
    void savingsCannotOverdraw() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.SAVINGS, 0, DEFAULT_RULES,
            true, Amount.ofEuros(50), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Savings accounts cannot overdraw"));
    }

    @Test
    void savingsCanTransferExactBalance() {
        // Source=200, transfer=199.50 → deduction=199.50+0.50=200 → newBalance=0 (exact, savings allowed)
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(199.50), true, AccountType.SAVINGS, 0, DEFAULT_RULES,
            true, Amount.ofEuros(200), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(0.0, result.newSourceBalance().toDouble(), 0.001);
    }

    @Test
    void feeIncreasesDeduction() {
        var rules = new TransferDecision.FeeRules(5.00, 1.00, 0.05, 1.00, 50.00, 10_000.0, 0.1, 6, 2.00);
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 0, rules,
            true, Amount.ofEuros(200), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        // Source: 200 - 100 - 5 = 95, Dest receives 100
        assertEquals(95.0, result.newSourceBalance().toDouble(), 0.001);
        assertEquals(400.0, result.newDestinationBalance().toDouble(), 0.001);
        assertEquals(5.00, result.totalFee().toDouble(), 0.001);
    }

    @Test
    void feeCausesRejectionInSavings() {
        var rules = new TransferDecision.FeeRules(1.00, 1.00, 0.05, 1.00, 50.00, 10_000.0, 0.1, 6, 2.00);
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(100), true, AccountType.SAVINGS, 0, rules,
            true, Amount.ofEuros(100), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("cannot overdraw"));
    }

    @Test
    void minimumBalanceConstantIsCorrect() {
        assertEquals(-1000.0, TransferDecision.CHECKING_MIN_BALANCE.toDouble(), 0.001);
    }

    @Test
    void externalTransferIncludesFeeBreakdown() {
        var facts = new TransferDecision.Facts(
            Amount.ofEuros(500), false, AccountType.CHECKING, 0, DEFAULT_RULES,
            true, Amount.ofEuros(1000), true, DEST_BALANCE);
        var result = TransferDecision.decide(facts);
        assertTrue(result.allowed());
        // External: 1.00 + 500*0.05% = 1.25
        assertEquals(1.25, result.totalFee().toDouble(), 0.01);
        assertEquals(1.25, result.baseFee().toDouble(), 0.01);
        assertEquals(0.0, result.excessPenalty().toDouble(), 0.001);
        assertEquals(0.0, result.transactionTax().toDouble(), 0.001);
    }
}
