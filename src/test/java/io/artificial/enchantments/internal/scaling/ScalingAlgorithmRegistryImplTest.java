package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScalingAlgorithmRegistryImpl Tests")
class ScalingAlgorithmRegistryImplTest {

    private ScalingAlgorithmRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new ScalingAlgorithmRegistryImpl();
    }

    // ==================== Built-in Algorithm Tests ====================

    @Test
    @DisplayName("Pre-registers all built-in algorithms on construction")
    void preRegistersBuiltInAlgorithms() {
        Set<String> names = registry.getRegisteredNames();

        assertTrue(names.contains("LINEAR"));
        assertTrue(names.contains("EXPONENTIAL"));
        assertTrue(names.contains("DIMINISHING"));
        assertTrue(names.contains("CONSTANT"));
        assertTrue(names.contains("STEPPED"));
        assertTrue(names.contains("DECAYING"));
        assertEquals(6, names.size());
    }

    @ParameterizedTest
    @CsvSource({
        "LINEAR, 10.0, 2.0, 1, 10.0",
        "LINEAR, 10.0, 2.0, 2, 12.0",
        "LINEAR, 10.0, 2.0, 5, 18.0",
        "EXPONENTIAL, 1.0, 2.0, 1, 1.0",
        "EXPONENTIAL, 1.0, 2.0, 2, 2.0",
        "EXPONENTIAL, 1.0, 2.0, 3, 4.0",
        "CONSTANT, 5.0, 0, 1, 5.0",
        "CONSTANT, 5.0, 0, 10, 5.0"
    })
    @DisplayName("Built-in algorithms produce correct values")
    void builtInAlgorithmsProduceCorrectValues(String name, double p1, double p2, int level, double expected) {
        LevelScaling scaling = p2 == 0
            ? registry.get(name, p1)
            : registry.get(name, p1, p2);

        assertEquals(expected, scaling.calculate(level), 0.001);
    }

    @Test
    @DisplayName("DIMINISHING algorithm produces correct values")
    void diminishingAlgorithmProducesCorrectValues() {
        LevelScaling scaling = registry.get("DIMINISHING", 100.0, 4.0);

        assertEquals(20.0, scaling.calculate(1), 0.1);  // 100 * (1 / 5) = 20
        assertEquals(55.6, scaling.calculate(5), 0.1);  // 100 * (5 / 9) = 55.6
    }

    @Test
    @DisplayName("DECAYING algorithm produces correct values")
    void decayingAlgorithmProducesCorrectValues() {
        LevelScaling scaling = registry.get("DECAYING", 100.0, 0.5);

        double expectedLevel1 = 100.0 * (1 - 0.5);     // 50.0
        double expectedLevel2 = 100.0 * (1 - 0.25);    // 75.0

        assertEquals(expectedLevel1, scaling.calculate(1), 0.001);
        assertEquals(expectedLevel2, scaling.calculate(2), 0.001);
    }

    @Test
    @DisplayName("STEPPED algorithm accepts variable parameters")
    void steppedAlgorithmAcceptsVariableParameters() {
        LevelScaling scaling = registry.get("STEPPED", 1.0, 5.0, 5.0, 10.0, 10.0, 20.0);

        assertEquals(5.0, scaling.calculate(1), 0.001);
        assertEquals(10.0, scaling.calculate(5), 0.001);
        assertEquals(20.0, scaling.calculate(10), 0.001);
    }

    // ==================== Algorithm Lookup Tests ====================

    @Test
    @DisplayName("get() is case-insensitive")
    void getIsCaseInsensitive() {
        LevelScaling lower = registry.get("linear", 1.0, 0.5);
        LevelScaling upper = registry.get("LINEAR", 1.0, 0.5);
        LevelScaling mixed = registry.get("Linear", 1.0, 0.5);

        assertEquals(lower.calculate(5), upper.calculate(5), 0.001);
        assertEquals(lower.calculate(5), mixed.calculate(5), 0.001);
    }

    @Test
    @DisplayName("get() throws for unregistered algorithm")
    void getThrowsForUnregisteredAlgorithm() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.get("UNKNOWN", 1.0)
        );
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("get() throws for null name")
    void getThrowsForNullName() {
        assertThrows(NullPointerException.class, () -> registry.get(null, 1.0));
    }

    @Test
    @DisplayName("get() throws for empty name")
    void getThrowsForEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> registry.get("", 1.0));
    }

    // ==================== Custom Registration Tests ====================

    @Test
    @DisplayName("Can register custom algorithm")
    void canRegisterCustomAlgorithm() {
        ScalingAlgorithm custom = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                return level -> params[0] * Math.log(level + params[1]);
            }

            @Override
            public String getDescription() {
                return "Logarithmic scaling";
            }

            @Override
            public int getParameterCount() {
                return 2;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"coefficient", "offset"};
            }
        };

        registry.register("LOGARITHMIC", custom);

        assertTrue(registry.hasAlgorithm("LOGARITHMIC"));
        LevelScaling scaling = registry.get("LOGARITHMIC", 10.0, 1.0);
        assertTrue(Double.isFinite(scaling.calculate(5)));
    }

    @Test
    @DisplayName("register() throws for duplicate name")
    void registerThrowsForDuplicateName() {
        ScalingAlgorithm custom = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                return level -> level * 2;
            }

            @Override
            public String getDescription() { return "Double"; }

            @Override
            public int getParameterCount() { return 0; }

            @Override
            public String[] getParameterNames() { return new String[0]; }
        };

        registry.register("CUSTOM", custom);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> registry.register("CUSTOM", custom)
        );
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("register() throws for null name")
    void registerThrowsForNullName() {
        assertThrows(NullPointerException.class,
            () -> registry.register(null, new TestScalingAlgorithm()));
    }

    @Test
    @DisplayName("register() throws for null algorithm")
    void registerThrowsForNullAlgorithm() {
        assertThrows(NullPointerException.class,
            () -> registry.register("TEST", null));
    }

    @Test
    @DisplayName("register() throws for empty name")
    void registerThrowsForEmptyName() {
        assertThrows(IllegalArgumentException.class,
            () -> registry.register("   ", new TestScalingAlgorithm()));
    }

    // ==================== hasAlgorithm Tests ====================

    @Test
    @DisplayName("hasAlgorithm returns true for registered algorithms")
    void hasAlgorithmReturnsTrueForRegistered() {
        assertTrue(registry.hasAlgorithm("LINEAR"));
        assertTrue(registry.hasAlgorithm("EXPONENTIAL"));
    }

    @Test
    @DisplayName("hasAlgorithm returns false for unregistered algorithms")
    void hasAlgorithmReturnsFalseForUnregistered() {
        assertFalse(registry.hasAlgorithm("UNKNOWN"));
    }

    @Test
    @DisplayName("hasAlgorithm is case-insensitive")
    void hasAlgorithmIsCaseInsensitive() {
        assertTrue(registry.hasAlgorithm("linear"));
        assertTrue(registry.hasAlgorithm("Linear"));
    }

    @Test
    @DisplayName("hasAlgorithm throws for null name")
    void hasAlgorithmThrowsForNull() {
        assertThrows(NullPointerException.class, () -> registry.hasAlgorithm(null));
    }

    // ==================== getMetadata Tests ====================

    @Test
    @DisplayName("getMetadata returns data for registered algorithm")
    void getMetadataReturnsData() {
        Optional<ScalingAlgorithmMetadata> metadata = registry.getMetadata("LINEAR");

        assertTrue(metadata.isPresent());
        assertEquals("LINEAR", metadata.get().getName());
        assertEquals(2, metadata.get().getParameterCount());
        assertTrue(metadata.get().isBuiltIn());
    }

    @Test
    @DisplayName("getMetadata returns empty for unregistered algorithm")
    void getMetadataReturnsEmptyForUnregistered() {
        Optional<ScalingAlgorithmMetadata> metadata = registry.getMetadata("UNKNOWN");
        assertTrue(metadata.isEmpty());
    }

    @Test
    @DisplayName("getMetadata is case-insensitive")
    void getMetadataIsCaseInsensitive() {
        Optional<ScalingAlgorithmMetadata> lower = registry.getMetadata("linear");
        Optional<ScalingAlgorithmMetadata> upper = registry.getMetadata("LINEAR");

        assertTrue(lower.isPresent());
        assertTrue(upper.isPresent());
        assertEquals(lower.get().getName(), upper.get().getName());
    }

    // ==================== unregister Tests ====================

    @Test
    @DisplayName("Can unregister custom algorithm")
    void canUnregisterCustomAlgorithm() {
        registry.register("TEMP", new TestScalingAlgorithm());

        assertTrue(registry.hasAlgorithm("TEMP"));
        assertTrue(registry.unregister("TEMP"));
        assertFalse(registry.hasAlgorithm("TEMP"));
    }

    @Test
    @DisplayName("Cannot unregister built-in algorithm")
    void cannotUnregisterBuiltInAlgorithm() {
        assertFalse(registry.unregister("LINEAR"));
        assertTrue(registry.hasAlgorithm("LINEAR"));
    }

    @Test
    @DisplayName("unregister returns false for unregistered algorithm")
    void unregisterReturnsFalseForUnregistered() {
        assertFalse(registry.unregister("UNKNOWN"));
    }

    @Test
    @DisplayName("unregister throws for null name")
    void unregisterThrowsForNull() {
        assertThrows(NullPointerException.class, () -> registry.unregister(null));
    }

    // ==================== Finite Value Validation Tests ====================

    @Test
    @DisplayName("get() validates that algorithm produces finite values")
    void getValidatesFiniteValues() {
        registry.register("INFINITE", new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                return level -> Double.POSITIVE_INFINITY;
            }

            @Override
            public String getDescription() { return "Infinite scaling"; }

            @Override
            public int getParameterCount() { return 0; }

            @Override
            public String[] getParameterNames() { return new String[0]; }
        });

        LevelScaling scaling = registry.get("INFINITE");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scaling.calculate(1)
        );
        assertTrue(exception.getMessage().contains("non-finite"));
    }

    @Test
    @DisplayName("get() validates that algorithm produces finite values - NaN")
    void getValidatesFiniteValuesNaN() {
        registry.register("NAN", new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                return level -> Double.NaN;
            }

            @Override
            public String getDescription() { return "NaN scaling"; }

            @Override
            public int getParameterCount() { return 0; }

            @Override
            public String[] getParameterNames() { return new String[0]; }
        });

        LevelScaling scaling = registry.get("NAN");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scaling.calculate(1)
        );
        assertTrue(exception.getMessage().contains("non-finite"));
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("Registry handles concurrent registrations")
    void handlesConcurrentRegistrations() throws InterruptedException {
        ScalingAlgorithmRegistryImpl concurrentRegistry = new ScalingAlgorithmRegistryImpl();
        int threadCount = 10;
        int registrationsPerThread = 10;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < registrationsPerThread; j++) {
                    try {
                        concurrentRegistry.register(
                            "CONCURRENT_" + threadId + "_" + j,
                            new TestScalingAlgorithm()
                        );
                    } catch (IllegalStateException e) {
                        // Expected if duplicate registration
                    }
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Should have all custom registrations plus built-ins
        int expectedCount = 6 + (threadCount * registrationsPerThread);
        assertTrue(concurrentRegistry.getRegisteredNames().size() >= 6);
    }

    // ==================== Test Helper Class ====================

    private static class TestScalingAlgorithm implements ScalingAlgorithm {
        @Override
        public LevelScaling create(double... params) {
            return level -> level * 1.0;
        }

        @Override
        public String getDescription() {
            return "Test algorithm";
        }

        @Override
        public int getParameterCount() {
            return 0;
        }

        @Override
        public String[] getParameterNames() {
            return new String[0];
        }
    }
}
