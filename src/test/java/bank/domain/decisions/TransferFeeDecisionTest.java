package bank.domain.decisions;

import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 */
class TransferFeeDecisionTest {

    private static final TransferFeeDecision.FeeRules DEFAULT_RULES =
        new TransferFeeDecision.FeeRules(0.50, 1.00, 0.05, 1.00, 50.00, 10_000.0, 0.1, 6, 2.00);

    @Test
    void internalTransferHasFlatFee() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(0.50, result.baseFee().toDouble(), 0.001);
        assertEquals(0.0, result.excessPenalty().toDouble(), 0.001);
        assertEquals(0.0, result.transactionTax().toDouble(), 0.001);
        assertEquals(0.50, result.totalFee().toDouble(), 0.001);
    }

    @Test
    void externalTransferHasFlatPlusPercent() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(1000), false, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(1.50, result.totalFee().toDouble(), 0.01);
    }

    @Test
    void externalFeeClampedToMinimum() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(1), false, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertTrue(result.totalFee().toDouble() >= 1.00);
    }

    @Test
    void externalFeeClampedToMaximum() {
        var rules = new TransferFeeDecision.FeeRules(0.50, 1.00, 10.0, 1.00, 50.00, 10_000.0, 0.1, 6, 2.00);
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(10000), false, AccountType.CHECKING, 0, rules);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(50.00, result.totalFee().toDouble(), 0.01);
    }

    @Test
    void savingsExcessPenaltyAfterFreeTransfers() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(100), true, AccountType.SAVINGS, 6, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(2.00, result.excessPenalty().toDouble(), 0.001);
    }

    @Test
    void savingsNoPenaltyWithinFreeTransfers() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(100), true, AccountType.SAVINGS, 5, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(0.0, result.excessPenalty().toDouble(), 0.001);
    }

    @Test
    void checkingNeverHasPenalty() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(100), true, AccountType.CHECKING, 100, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(0.0, result.excessPenalty().toDouble(), 0.001);
    }

    @Test
    void transactionTaxOnLargeExternalTransfer() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(15000), false, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(5.00, result.transactionTax().toDouble(), 0.01);
    }

    @Test
    void noTransactionTaxOnInternalTransfer() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(50000), true, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(0.0, result.transactionTax().toDouble(), 0.001);
    }

    @Test
    void noTransactionTaxBelowThreshold() {
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(10000), false, AccountType.CHECKING, 0, DEFAULT_RULES);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(0.0, result.transactionTax().toDouble(), 0.001);
    }

    @Test
    void configurableFreeTransferCountIsUsed() {
        var rules = new TransferFeeDecision.FeeRules(0.50, 1.00, 0.05, 1.00, 50.00, 10_000.0, 0.1, 3, 5.00);
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(100), true, AccountType.SAVINGS, 3, rules);
        var result = TransferFeeDecision.decide(facts);
        assertEquals(5.00, result.excessPenalty().toDouble(), 0.001);
    }

    @Test
    void configurableMinMaxExternalFeeIsUsed() {
        var rules = new TransferFeeDecision.FeeRules(0.50, 1.00, 5.0, 0.50, 20.00, 10_000.0, 0.1, 6, 2.00);
        var facts = new TransferFeeDecision.Facts(
            Amount.ofEuros(1000), false, AccountType.CHECKING, 0, rules);
        var result = TransferFeeDecision.decide(facts);
        // flat 1.00 + 1000*5% = 51.00 → clamped to 20.00
        assertEquals(20.00, result.totalFee().toDouble(), 0.01);
    }
}
