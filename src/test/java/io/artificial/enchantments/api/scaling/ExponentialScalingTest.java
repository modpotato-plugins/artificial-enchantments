package io.artificial.enchantments.api.scaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExponentialScaling Tests")
class ExponentialScalingTest {

    @Test
    @DisplayName("Calculate at level 1 returns base value")
    void calculateLevel1ReturnsBase() {
        ExponentialScaling scaling = new ExponentialScaling(2.0, 1.5);
        
        assertEquals(2.0, scaling.calculate(1), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "2.0, 1.5, 2, 3.0",
        "2.0, 1.5, 3, 4.5",
        "2.0, 1.5, 5, 10.125",
        "10.0, 2.0, 2, 20.0",
        "10.0, 2.0, 3, 40.0"
    })
    @DisplayName("Calculate at various levels works correctly")
    void calculateAtVariousLevels(double base, double multiplier, int level, double expected) {
        ExponentialScaling scaling = new ExponentialScaling(base, multiplier);
        
        assertEquals(expected, scaling.calculate(level), 0.001);
    }

    @Test
    @DisplayName("Multiplier of 1.0 returns constant base value")
    void multiplierOfOneReturnsConstant() {
        ExponentialScaling scaling = new ExponentialScaling(5.0, 1.0);
        
        assertEquals(5.0, scaling.calculate(1), 0.001);
        assertEquals(5.0, scaling.calculate(5), 0.001);
        assertEquals(5.0, scaling.calculate(100), 0.001);
    }

    @Test
    @DisplayName("Multiplier less than 1 decreases value")
    void fractionalMultiplierDecreasesValue() {
        ExponentialScaling scaling = new ExponentialScaling(10.0, 0.5);
        
        assertEquals(10.0, scaling.calculate(1), 0.001);
        assertEquals(5.0, scaling.calculate(2), 0.001);
        assertEquals(2.5, scaling.calculate(3), 0.001);
        assertEquals(1.25, scaling.calculate(4), 0.001);
    }

    @Test
    @DisplayName("Multiplier greater than 1 increases value rapidly")
    void largeMultiplierIncreasesRapidly() {
        ExponentialScaling scaling = new ExponentialScaling(1.0, 2.0);
        
        assertEquals(1.0, scaling.calculate(1), 0.001);
        assertEquals(2.0, scaling.calculate(2), 0.001);
        assertEquals(4.0, scaling.calculate(3), 0.001);
        assertEquals(512.0, scaling.calculate(10), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("Invalid level throws IllegalArgumentException")
    void invalidLevelThrows(int invalidLevel) {
        ExponentialScaling scaling = new ExponentialScaling(1.0, 1.5);
        
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(invalidLevel));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -0.5, -100.0})
    @DisplayName("Non-positive multiplier throws IllegalArgumentException")
    void nonPositiveMultiplierThrows(double invalidMultiplier) {
        assertThrows(IllegalArgumentException.class, () -> new ExponentialScaling(1.0, invalidMultiplier));
    }

    @Test
    @DisplayName("GetBase returns correct value")
    void getBaseReturnsCorrectValue() {
        ExponentialScaling scaling = new ExponentialScaling(15.5, 2.0);
        
        assertEquals(15.5, scaling.getBase(), 0.001);
    }

    @Test
    @DisplayName("GetMultiplier returns correct value")
    void getMultiplierReturnsCorrectValue() {
        ExponentialScaling scaling = new ExponentialScaling(10.0, 1.75);
        
        assertEquals(1.75, scaling.getMultiplier(), 0.001);
    }

    @Test
    @DisplayName("ToString contains expected values")
    void toStringContainsExpectedValues() {
        ExponentialScaling scaling = new ExponentialScaling(5.0, 1.5);
        String result = scaling.toString();
        
        assertTrue(result.contains("ExponentialScaling"));
        assertTrue(result.contains("5.0") || result.contains("5.00"));
        assertTrue(result.contains("1.5") || result.contains("1.50"));
    }

    @Test
    @DisplayName("Large level calculations work correctly")
    void largeLevelCalculationsWork() {
        ExponentialScaling scaling = new ExponentialScaling(1.0, 1.01);
        
        assertEquals(1.0 * Math.pow(1.01, 99), scaling.calculate(100), 0.001);
    }
}
