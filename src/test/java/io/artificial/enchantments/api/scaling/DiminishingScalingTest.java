package io.artificial.enchantments.api.scaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiminishingScaling Tests")
class DiminishingScalingTest {

    @ParameterizedTest
    @CsvSource({
        "10.0, 5.0, 1, 1.6667",
        "10.0, 5.0, 5, 5.0",
        "10.0, 5.0, 10, 6.6667",
        "100.0, 10.0, 1, 9.0909",
        "100.0, 10.0, 10, 50.0",
        "100.0, 10.0, 90, 90.0"
    })
    @DisplayName("Calculate at various levels works correctly")
    void calculateAtVariousLevels(double maxValue, double scalingFactor, int level, double expected) {
        DiminishingScaling scaling = new DiminishingScaling(maxValue, scalingFactor);
        
        assertEquals(expected, scaling.calculate(level), 0.001);
    }

    @Test
    @DisplayName("Value approaches max asymptotically")
    void valueApproachesMaxAsymptotically() {
        DiminishingScaling scaling = new DiminishingScaling(10.0, 5.0);
        
        double level1 = scaling.calculate(1);
        double level10 = scaling.calculate(10);
        double level100 = scaling.calculate(100);
        double level1000 = scaling.calculate(1000);
        
        assertTrue(level1 < level10, "Higher levels should have higher values");
        assertTrue(level10 < level100, "Higher levels should have higher values");
        assertTrue(level100 < level1000, "Higher levels should have higher values");
        
        assertTrue(level1 < 10.0, "Should never reach max");
        assertTrue(level10 < 10.0, "Should never reach max");
        assertTrue(level100 < 10.0, "Should never reach max");
        assertTrue(level1000 < 10.0, "Should never reach max");
        
        assertTrue(level1000 > 9.9, "Should be very close to max at high levels");
    }

    @Test
    @DisplayName("Higher scaling factor means slower approach to max")
    void higherScalingFactorSlowerApproach() {
        DiminishingScaling slow = new DiminishingScaling(10.0, 20.0);
        DiminishingScaling fast = new DiminishingScaling(10.0, 2.0);
        
        double slowLevel10 = slow.calculate(10);
        double fastLevel10 = fast.calculate(10);
        
        assertTrue(slowLevel10 < fastLevel10, "Higher scaling factor should approach max slower");
    }

    @Test
    @DisplayName("Level equals scaling factor returns half max")
    void levelEqualsScalingFactorReturnsHalfMax() {
        DiminishingScaling scaling = new DiminishingScaling(10.0, 5.0);
        
        assertEquals(5.0, scaling.calculate(5), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("Invalid level throws IllegalArgumentException")
    void invalidLevelThrows(int invalidLevel) {
        DiminishingScaling scaling = new DiminishingScaling(10.0, 5.0);
        
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(invalidLevel));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -0.01})
    @DisplayName("Non-positive maxValue throws IllegalArgumentException")
    void nonPositiveMaxValueThrows(double invalidMaxValue) {
        assertThrows(IllegalArgumentException.class, () -> new DiminishingScaling(invalidMaxValue, 5.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -0.01})
    @DisplayName("Non-positive scalingFactor throws IllegalArgumentException")
    void nonPositiveScalingFactorThrows(double invalidFactor) {
        assertThrows(IllegalArgumentException.class, () -> new DiminishingScaling(10.0, invalidFactor));
    }

    @Test
    @DisplayName("GetMaxValue returns correct value")
    void getMaxValueReturnsCorrectValue() {
        DiminishingScaling scaling = new DiminishingScaling(15.5, 2.0);
        
        assertEquals(15.5, scaling.getMaxValue(), 0.001);
    }

    @Test
    @DisplayName("GetScalingFactor returns correct value")
    void getScalingFactorReturnsCorrectValue() {
        DiminishingScaling scaling = new DiminishingScaling(10.0, 7.5);
        
        assertEquals(7.5, scaling.getScalingFactor(), 0.001);
    }

    @Test
    @DisplayName("ToString contains expected values")
    void toStringContainsExpectedValues() {
        DiminishingScaling scaling = new DiminishingScaling(10.0, 5.0);
        String result = scaling.toString();
        
        assertTrue(result.contains("DiminishingScaling"));
        assertTrue(result.contains("10.0") || result.contains("10.00"));
        assertTrue(result.contains("5.0") || result.contains("5.00"));
    }

    @Test
    @DisplayName("Fractional max value works correctly")
    void fractionalMaxValueWorks() {
        DiminishingScaling scaling = new DiminishingScaling(0.5, 0.1);
        
        assertTrue(scaling.calculate(1) > 0);
        assertTrue(scaling.calculate(1) < 0.5);
    }

    @Test
    @DisplayName("Very large levels approach but never exceed max")
    void veryLargeLevelsApproachMax() {
        DiminishingScaling scaling = new DiminishingScaling(100.0, 1.0);
        
        double veryHigh = scaling.calculate(1000000);
        
        assertTrue(veryHigh < 100.0, "Should never exceed max");
        assertTrue(veryHigh > 99.99, "Should be extremely close to max");
    }
}
