package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.LevelScaling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScalingUtils Tests")
class ScalingUtilsTest {

    @Test
    @DisplayName("Linear scaling calculates correctly at various levels")
    void linearScalingCalculatesCorrectly() {
        LevelScaling scaling = ScalingUtils.linear(1.0, 0.5);
        
        assertEquals(1.0, scaling.calculate(1), 0.001, "Level 1 should return base value");
        assertEquals(1.5, scaling.calculate(2), 0.001, "Level 2 should add one increment");
        assertEquals(3.0, scaling.calculate(5), 0.001, "Level 5: 1.0 + 4*0.5 = 3.0");
        assertEquals(5.5, scaling.calculate(10), 0.001, "Level 10: 1.0 + 9*0.5 = 5.5");
    }

    @Test
    @DisplayName("Linear scaling with zero increment is constant")
    void linearScalingWithZeroIncrementIsConstant() {
        LevelScaling scaling = ScalingUtils.linear(5.0, 0.0);
        
        assertEquals(5.0, scaling.calculate(1), 0.001);
        assertEquals(5.0, scaling.calculate(5), 0.001);
        assertEquals(5.0, scaling.calculate(100), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Linear scaling throws on invalid level")
    void linearScalingThrowsOnInvalidLevel(int invalidLevel) {
        LevelScaling scaling = ScalingUtils.linear(1.0, 0.5);
        
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(invalidLevel));
    }

    @Test
    @DisplayName("Exponential scaling calculates correctly at various levels")
    void exponentialScalingCalculatesCorrectly() {
        LevelScaling scaling = ScalingUtils.exponential(2.0, 1.5);
        
        assertEquals(2.0, scaling.calculate(1), 0.001, "Level 1: 2.0 * 1.5^0 = 2.0");
        assertEquals(3.0, scaling.calculate(2), 0.001, "Level 2: 2.0 * 1.5^1 = 3.0");
        assertEquals(10.125, scaling.calculate(5), 0.001, "Level 5: 2.0 * 1.5^4 = 10.125");
    }

    @Test
    @DisplayName("Exponential scaling with multiplier 1 is constant")
    void exponentialScalingWithMultiplierOneIsConstant() {
        LevelScaling scaling = ScalingUtils.exponential(5.0, 1.0);
        
        assertEquals(5.0, scaling.calculate(1), 0.001);
        assertEquals(5.0, scaling.calculate(10), 0.001);
        assertEquals(5.0, scaling.calculate(100), 0.001);
    }

    @Test
    @DisplayName("Exponential scaling throws on non-positive multiplier")
    void exponentialScalingThrowsOnNonPositiveMultiplier() {
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.exponential(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.exponential(1.0, -1.0));
    }

    @Test
    @DisplayName("Diminishing scaling approaches max asymptotically")
    void diminishingScalingApproachesMaxAsymptotically() {
        LevelScaling scaling = ScalingUtils.diminishing(10.0, 5.0);
        
        assertEquals(10.0 * (1.0 / 6.0), scaling.calculate(1), 0.001, "Level 1");
        assertEquals(5.0, scaling.calculate(5), 0.001, "Level 5: 10.0 * 5/10 = 5.0");
        assertEquals(10.0 * (10.0 / 15.0), scaling.calculate(10), 0.001, "Level 10");
        
        // At high levels, should approach but never reach max
        double highLevelValue = scaling.calculate(100);
        assertTrue(highLevelValue < 10.0, "Should never reach max");
        assertTrue(highLevelValue > 9.5, "Should be close to max at level 100");
    }

    @Test
    @DisplayName("Diminishing scaling throws on invalid parameters")
    void diminishingScalingThrowsOnInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.diminishing(0.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.diminishing(-1.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.diminishing(10.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.diminishing(10.0, -1.0));
    }

    @Test
    @DisplayName("Constant scaling returns same value for all levels")
    void constantScalingReturnsSameValue() {
        LevelScaling scaling = ScalingUtils.constant(7.5);
        
        assertEquals(7.5, scaling.calculate(1), 0.001);
        assertEquals(7.5, scaling.calculate(50), 0.001);
        assertEquals(7.5, scaling.calculate(999), 0.001);
    }

    @Test
    @DisplayName("Custom function scaling works with user-defined formula")
    void customFunctionScalingWorks() {
        // Quadratic scaling: value = level^2
        LevelScaling quadratic = ScalingUtils.custom(level -> (double) level * level);
        
        assertEquals(1.0, quadratic.calculate(1), 0.001);
        assertEquals(25.0, quadratic.calculate(5), 0.001);
        assertEquals(100.0, quadratic.calculate(10), 0.001);
        
        // Logarithmic scaling: value = log(level + 1)
        LevelScaling logScale = ScalingUtils.custom(level -> Math.log(level + 1));
        
        assertEquals(Math.log(2), logScale.calculate(1), 0.001);
        assertEquals(Math.log(6), logScale.calculate(5), 0.001);
    }

    @Test
    @DisplayName("Decaying scaling approaches max asymptotically from below")
    void decayingScalingApproachesMax() {
        LevelScaling scaling = ScalingUtils.decaying(10.0, 0.9);
        
        assertEquals(1.0, scaling.calculate(1), 0.001, "Level 1: 10.0 * (1 - 0.9) = 1.0");
        
        // Level 5: 10.0 * (1 - 0.9^5) = 10.0 * (1 - 0.59049) = 4.0951
        assertEquals(4.0951, scaling.calculate(5), 0.001, "Level 5");
        
        // At high levels, should approach max
        double highLevelValue = scaling.calculate(100);
        assertTrue(highLevelValue > 9.9, "Should be very close to max at level 100");
        assertTrue(highLevelValue < 10.0, "Should never exceed max");
    }

    @Test
    @DisplayName("Decaying scaling throws on invalid parameters")
    void decayingScalingThrowsOnInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.decaying(0.0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.decaying(10.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.decaying(10.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.decaying(10.0, 1.5));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "1.4, 1",
        "1.5, 2",
        "1.6, 2",
        "2.0, 2"
    })
    @DisplayName("Calculate rounded returns correct integer")
    void calculateRoundedReturnsCorrectInteger(double input, int expected) {
        LevelScaling scaling = level -> input;
        
        assertEquals(expected, ScalingUtils.calculateRounded(scaling, 1));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "1.9, 1",
        "2.0, 2",
        "2.1, 2"
    })
    @DisplayName("Calculate floored returns correct integer")
    void calculateFlooredReturnsCorrectInteger(double input, int expected) {
        LevelScaling scaling = level -> input;
        
        assertEquals(expected, ScalingUtils.calculateFloored(scaling, 1));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1",
        "1.1, 2",
        "2.0, 2"
    })
    @DisplayName("Calculate ceiled returns correct integer")
    void calculateCeiledReturnsCorrectInteger(double input, int expected) {
        LevelScaling scaling = level -> input;
        
        assertEquals(expected, ScalingUtils.calculateCeiled(scaling, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Validate level throws on invalid levels")
    void validateLevelThrowsOnInvalidLevels(int invalidLevel) {
        assertThrows(IllegalArgumentException.class, () -> ScalingUtils.validateLevel(invalidLevel));
    }

    @Test
    @DisplayName("Validate level accepts valid levels")
    void validateLevelAcceptsValidLevels() {
        assertDoesNotThrow(() -> ScalingUtils.validateLevel(1));
        assertDoesNotThrow(() -> ScalingUtils.validateLevel(5));
        assertDoesNotThrow(() -> ScalingUtils.validateLevel(1000));
    }

    @Test
    @DisplayName("Linear scaling with negative increment decreases")
    void linearScalingWithNegativeIncrement() {
        LevelScaling scaling = ScalingUtils.linear(10.0, -1.0);
        
        assertEquals(10.0, scaling.calculate(1), 0.001);
        assertEquals(9.0, scaling.calculate(2), 0.001);
        assertEquals(6.0, scaling.calculate(5), 0.001);
    }

    @Test
    @DisplayName("Exponential scaling with fractional multiplier")
    void exponentialScalingWithFractionalMultiplier() {
        LevelScaling scaling = ScalingUtils.exponential(10.0, 0.5);
        
        assertEquals(10.0, scaling.calculate(1), 0.001);
        assertEquals(5.0, scaling.calculate(2), 0.001);
        assertEquals(1.25, scaling.calculate(4), 0.001);
    }
}
