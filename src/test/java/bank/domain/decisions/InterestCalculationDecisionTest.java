package bank.domain.decisions;

import bank.domain.facts.Amount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 */
class InterestCalculationDecisionTest {

    private static final InterestCalculationDecision.InterestRules DEFAULT_RULES =
        new InterestCalculationDecision.InterestRules(2.0, 25.0, 100.0);

    @Test
    void calculateInterestForSavingsAccount() {
        // 10000€ at 2% for 30 days → ~16.44€ gross
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(10000), 30, Amount.ZERO, DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        assertTrue(result.grossInterest().compareTo(Amount.ZERO) > 0);
        assertTrue(result.netInterest().compareTo(Amount.ZERO) > 0);
    }

    @Test
    void zeroBalanceProducesZeroInterest() {
        var facts = new InterestCalculationDecision.Facts(
            Amount.ZERO, 30, Amount.ZERO, DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        assertEquals(0.0, result.grossInterest().toDouble(), 0.001);
        assertEquals(0.0, result.netInterest().toDouble(), 0.001);
    }

    @Test
    void zeroDaysProducesZeroInterest() {
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(10000), 0, Amount.ZERO, DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        assertEquals(0.0, result.grossInterest().toDouble(), 0.001);
    }

    @Test
    void noTaxWhenBelowExemption() {
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(10000), 30, Amount.ZERO, DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        // YTD = 0, so all interest fits under the 100€ exemption
        assertEquals(0.0, result.taxableInterest().toDouble(), 0.001);
        assertEquals(0.0, result.taxAmount().toDouble(), 0.001);
        assertEquals(result.grossInterest(), result.netInterest());
    }

    @Test
    void taxAppliedWhenExemptionExceeded() {
        // Already earned 120€ interest this year → exemption exhausted
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(20000), 90, Amount.ofEuros(120), DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        // All new interest is taxable
        assertTrue(result.taxableInterest().compareTo(Amount.ZERO) > 0);
        assertTrue(result.taxAmount().compareTo(Amount.ZERO) > 0);
        assertTrue(result.netInterest().compareTo(result.grossInterest()) < 0);
    }

    @Test
    void partialTaxWhenExemptionPartiallyUsed() {
        // 80€ already earned, 100€ exemption → 20€ remaining
        var rules = new InterestCalculationDecision.InterestRules(10.0, 25.0, 100.0);
        // 10000€ at 10% for 30 days → ~82.19€ gross
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(10000), 30, Amount.ofEuros(80), rules);
        var result = InterestCalculationDecision.decide(facts);
        // Taxable should be ~ (82.19 - 20) = 62.19€
        assertTrue(result.taxableInterest().compareTo(Amount.ZERO) > 0);
        assertTrue(result.taxAmount().compareTo(Amount.ZERO) > 0);
    }

    @Test
    void netEqualsGrossMinusTax() {
        var facts = new InterestCalculationDecision.Facts(
            Amount.ofEuros(50000), 365, Amount.ofEuros(200), DEFAULT_RULES);
        var result = InterestCalculationDecision.decide(facts);
        assertEquals(
            result.grossInterest().minus(result.taxAmount()).toDouble(),
            result.netInterest().toDouble(),
            0.01);
    }
}
