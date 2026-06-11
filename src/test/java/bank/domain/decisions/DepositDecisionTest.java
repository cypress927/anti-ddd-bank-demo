package bank.domain.decisions;

import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositDecisionTest {

    @Test
    void allowPositiveAmount() {
        var facts = new DepositDecision.Facts(
            Amount.ofEuros(100), Amount.ZERO, true);
        var result = DepositDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(100.0, result.newBalance().toDouble(), 0.001);
    }

    @Test
    void computeNewBalanceCorrectly() {
        var facts = new DepositDecision.Facts(
            Amount.ofEuros(50.50), Amount.ofEuros(200), true);
        var result = DepositDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(250.50, result.newBalance().toDouble(), 0.001);
    }

    @Test
    void rejectZeroAmount() {
        var facts = new DepositDecision.Facts(
            Amount.ZERO, Amount.ZERO, true);
        var result = DepositDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("greater than 0"));
    }

    @Test
    void rejectNegativeAmount() {
        var facts = new DepositDecision.Facts(
            Amount.ofEuros(-10), Amount.ofEuros(100), true);
        var result = DepositDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void rejectUnknownDestination() {
        var facts = new DepositDecision.Facts(
            Amount.ofEuros(100), Amount.ZERO, false);
        var result = DepositDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("does not exist"));
    }
}
