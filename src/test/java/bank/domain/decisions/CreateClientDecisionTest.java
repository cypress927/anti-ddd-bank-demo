package bank.domain.decisions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure business function test — no mocks, no Spring, no database.
 * Every test: construct plain Facts → call decide → assert plain Result.
 */
class CreateClientDecisionTest {

    @Test
    void allowValidUsername() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("jack", false));
        assertTrue(result.allowed());
        assertNull(result.reason());
    }

    @Test
    void rejectNullUsername() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts(null, false));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("null"));
    }

    @Test
    void rejectUsernameStartingWithDigit() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("012A", false));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("letter"));
    }

    @Test
    void rejectEmptyUsername() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("", false));
        assertFalse(result.allowed());
    }

    @Test
    void rejectDuplicateUsername() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("jack", true));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("already taken"));
    }

    @Test
    void allowUnderscoreAndDigits() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("user_123", false));
        assertTrue(result.allowed());
    }

    @Test
    void rejectUsernameWithSpecialChars() {
        var result = CreateClientDecision.decide(
            new CreateClientDecision.Facts("jack@sparrow", false));
        assertFalse(result.allowed());
    }
}
