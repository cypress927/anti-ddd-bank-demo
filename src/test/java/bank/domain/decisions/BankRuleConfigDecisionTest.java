package bank.domain.decisions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 */
class BankRuleConfigDecisionTest {

    @Test
    void allowValidInterestRate() {
        var facts = new BankRuleConfigDecision.Facts("interest.rate", 3.5);
        var result = BankRuleConfigDecision.decide(facts);
        assertTrue(result.allowed());
        assertEquals(3.5, result.effectiveValue(), 0.001);
    }

    @Test
    void rejectTooHighInterestRate() {
        var facts = new BankRuleConfigDecision.Facts("interest.rate", 25.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("out of range"));
    }

    @Test
    void rejectNegativeInterestRate() {
        var facts = new BankRuleConfigDecision.Facts("interest.rate", -1.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void allowBoundaryValues() {
        var facts1 = new BankRuleConfigDecision.Facts("interest.rate", 0.0);
        assertTrue(BankRuleConfigDecision.decide(facts1).allowed());

        var facts2 = new BankRuleConfigDecision.Facts("interest.rate", 20.0);
        assertTrue(BankRuleConfigDecision.decide(facts2).allowed());
    }

    @Test
    void rejectUnknownRuleKey() {
        var facts = new BankRuleConfigDecision.Facts("nonexistent.rule", 5.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Unknown rule key"));
    }

    @Test
    void validateTaxExemption() {
        var facts = new BankRuleConfigDecision.Facts("interest.tax.exemption", 500.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertTrue(result.allowed());
    }

    @Test
    void rejectTooHighTaxExemption() {
        var facts = new BankRuleConfigDecision.Facts("interest.tax.exemption", 20000.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertFalse(result.allowed());
    }

    @Test
    void validateTransferTaxThreshold() {
        var facts = new BankRuleConfigDecision.Facts("transfer.tax.threshold", 50000.0);
        var result = BankRuleConfigDecision.decide(facts);
        assertTrue(result.allowed());
    }

    @Test
    void rejectNegativeTransferTaxRate() {
        var facts = new BankRuleConfigDecision.Facts("transfer.tax.rate", -0.5);
        var result = BankRuleConfigDecision.decide(facts);
        assertFalse(result.allowed());
    }
}
