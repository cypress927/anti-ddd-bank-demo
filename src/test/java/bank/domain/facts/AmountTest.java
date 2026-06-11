package bank.domain.facts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmountTest {

    @Test
    void zeroIsZero() {
        assertEquals(0.0, Amount.ZERO.toDouble(), 0.0001);
    }

    @Test
    void ofEurosCreatesCorrectCents() {
        var a = Amount.ofEuros(100.50);
        assertEquals(10050, a.cents());
        assertEquals(100.50, a.toDouble(), 0.0001);
    }

    @Test
    void ofEurosCentsWorks() {
        var a = Amount.ofEurosCents(5, 25);
        assertEquals(525, a.cents());
        assertEquals(5.25, a.toDouble(), 0.0001);
    }

    @Test
    void plusWorks() {
        var a = Amount.ofEuros(100);
        var b = Amount.ofEuros(50.50);
        assertEquals(150.50, a.plus(b).toDouble(), 0.0001);
    }

    @Test
    void minusWorks() {
        var a = Amount.ofEuros(100);
        var b = Amount.ofEuros(30.25);
        assertEquals(69.75, a.minus(b).toDouble(), 0.0001);
    }

    @Test
    void timesWorks() {
        var a = Amount.ofEuros(100);
        assertEquals(300.0, a.times(3).toDouble(), 0.0001);
        assertEquals(-100.0, a.times(-1).toDouble(), 0.0001);
    }

    @Test
    void compareToWorks() {
        var a = Amount.ofEuros(100);
        var b = Amount.ofEuros(200);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(Amount.ofEuros(100)));
    }

    @Test
    void equalsAndHashCode() {
        var a1 = Amount.ofEuros(100);
        var a2 = Amount.ofEuros(100);
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, Amount.ofEuros(100.01));
    }

    @Test
    void rejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> Amount.ofEuros(Double.NaN));
    }

    @Test
    void rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Amount.ofEuros(1e14));
        assertThrows(IllegalArgumentException.class, () -> Amount.ofEuros(-1e14));
    }

    @Test
    void negativeAmountsWork() {
        var a = Amount.ofEuros(-1000);
        assertEquals(-100000, a.cents());
        assertEquals(-1000.0, a.toDouble(), 0.0001);
    }
}
