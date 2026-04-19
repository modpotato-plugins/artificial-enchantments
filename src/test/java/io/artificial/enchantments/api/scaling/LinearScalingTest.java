package io.artificial.enchantments.api.scaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LinearScaling Tests")
class LinearScalingTest {

    @Test
    @DisplayName("Calculate at level 1 returns base value")
    void calculateLevel1ReturnsBase() {
        LinearScaling scaling = new LinearScaling(10.0, 2.0);
        
        assertEquals(10.0, scaling.calculate(1), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 2.0, 2, 12.0",
        "10.0, 2.0, 5, 18.0",
        "10.0, 2.0, 10, 28.0",
        "5.0, 1.0, 3, 7.0",
        "0.0, 5.0, 5, 20.0"
    })
    @DisplayName("Calculate at various levels works correctly")
    void calculateAtVariousLevels(double base, double increment, int level, double expected) {
        LinearScaling scaling = new LinearScaling(base, increment);
        
        assertEquals(expected, scaling.calculate(level), 0.001);
    }

    @Test
    @DisplayName("Zero increment returns constant base value")
    void zeroIncrementReturnsConstant() {
        LinearScaling scaling = new LinearScaling(7.5, 0.0);
        
        assertEquals(7.5, scaling.calculate(1), 0.001);
        assertEquals(7.5, scaling.calculate(5), 0.001);
        assertEquals(7.5, scaling.calculate(100), 0.001);
    }

    @Test
    @DisplayName("Negative increment decreases value")
    void negativeIncrementDecreasesValue() {
        LinearScaling scaling = new LinearScaling(10.0, -1.0);
        
        assertEquals(10.0, scaling.calculate(1), 0.001);
        assertEquals(9.0, scaling.calculate(2), 0.001);
        assertEquals(6.0, scaling.calculate(5), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("Invalid level throws IllegalArgumentException")
    void invalidLevelThrows(int invalidLevel) {
        LinearScaling scaling = new LinearScaling(1.0, 0.5);
        
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(invalidLevel));
    }

    @Test
    @DisplayName("GetBase returns correct value")
    void getBaseReturnsCorrectValue() {
        LinearScaling scaling = new LinearScaling(15.5, 2.5);
        
        assertEquals(15.5, scaling.getBase(), 0.001);
    }

    @Test
    @DisplayName("GetIncrement returns correct value")
    void getIncrementReturnsCorrectValue() {
        LinearScaling scaling = new LinearScaling(10.0, 3.5);
        
        assertEquals(3.5, scaling.getIncrement(), 0.001);
    }

    @Test
    @DisplayName("ToString contains expected values")
    void toStringContainsExpectedValues() {
        LinearScaling scaling = new LinearScaling(5.0, 1.5);
        String result = scaling.toString();
        
        assertTrue(result.contains("LinearScaling"));
        assertTrue(result.contains("5.0") || result.contains("5.00"));
        assertTrue(result.contains("1.5") || result.contains("1.50"));
    }

    @Test
    @DisplayName("Large level calculations work correctly")
    void largeLevelCalculationsWork() {
        LinearScaling scaling = new LinearScaling(1.0, 0.1);
        
        assertEquals(10000.9, scaling.calculate(100000), 0.001);
    }

    @Test
    @DisplayName("Fractional values work correctly")
    void fractionalValuesWork() {
        LinearScaling scaling = new LinearScaling(0.5, 0.25);
        
        assertEquals(0.5, scaling.calculate(1), 0.001);
        assertEquals(0.75, scaling.calculate(2), 0.001);
        assertEquals(1.5, scaling.calculate(5), 0.001);
    }
}
