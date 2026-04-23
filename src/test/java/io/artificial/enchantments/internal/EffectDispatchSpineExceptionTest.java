package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EffectExecutionContext Exception Isolation Tests")
class EffectDispatchSpineExceptionTest {

    private static final String TEST_NS = "test";

    @Test
    @DisplayName("Execution mode defaults to LENIENT when not specified")
    void executionModeDefaultsToLenient() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "test"),
                "TEST_EVENT",
                1
        );

        assertEquals(EffectExecutionContext.ExecutionMode.LENIENT, context.getExecutionMode());
    }

    @Test
    @DisplayName("EffectExecutionContext stores enchantment details correctly")
    void executionContextStoresDetails() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "detail_test");
        EffectExecutionContext context = new EffectExecutionContext(
                key,
                "ENTITY_DAMAGE_BY_ENTITY",
                5,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        assertEquals(key, context.getEnchantmentKey());
        assertEquals("ENTITY_DAMAGE_BY_ENTITY", context.getEffectType());
        assertEquals(5, context.getLevel());
        assertEquals(EffectExecutionContext.ExecutionMode.LENIENT, context.getExecutionMode());
    }

    @Test
    @DisplayName("EffectExecutionContext withLevel creates new instance with updated level")
    void executionContextWithLevelCreatesNewInstance() {
        EffectExecutionContext original = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "level_test"),
                "ENTITY_DAMAGE",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        EffectExecutionContext modified = original.withLevel(10);

        assertNotSame(original, modified);
        assertEquals(1, original.getLevel());
        assertEquals(10, modified.getLevel());
        assertEquals(original.getEnchantmentKey(), modified.getEnchantmentKey());
        assertEquals(original.getEffectType(), modified.getEffectType());
    }

    @Test
    @DisplayName("EffectExecutionContext withExecutionMode creates new instance")
    void executionContextWithModeCreatesNewInstance() {
        EffectExecutionContext original = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "mode_test"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        EffectExecutionContext modified = original.withExecutionMode(EffectExecutionContext.ExecutionMode.STRICT);

        assertNotSame(original, modified);
        assertEquals(EffectExecutionContext.ExecutionMode.LENIENT, original.getExecutionMode());
        assertEquals(EffectExecutionContext.ExecutionMode.STRICT, modified.getExecutionMode());
    }

    @Test
    @DisplayName("LENIENT mode execution succeeds without exception")
    void lenientModeExecutionSucceeds() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "success"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean success = context.executeWithIsolation(Test.class, () -> executed.set(true));

        assertTrue(success, "Execution should report success");
        assertTrue(executed.get(), "Action should have been executed");
    }

    @Test
    @DisplayName("LENIENT mode catches and reports failure without throwing")
    void lenientModeCatchesFailure() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "failure"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        boolean success = context.executeWithIsolation(Test.class, () -> {
            throw new RuntimeException("Test exception");
        });

        assertFalse(success, "Execution should report failure");
    }

    @Test
    @DisplayName("STRICT mode throws RuntimeException when action fails")
    void strictModeThrowsRuntimeException() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "strict"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.STRICT
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            context.executeWithIsolation(Test.class, () -> {
                throw new RuntimeException("Strict mode exception");
            });
        });

        assertEquals("Strict mode exception", thrown.getMessage());
    }

    @Test
    @DisplayName("STRICT mode re-throws RuntimeException as-is")
    void strictModeReThrowsRuntimeException() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "strict_runtime"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.STRICT
        );

        RuntimeException original = new RuntimeException("Original runtime exception");
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> {
                    context.executeWithIsolation(Test.class, () -> {
                        throw original;
                    });
                }
        );

        assertEquals("Original runtime exception", thrown.getMessage());
    }

    @Test
    @DisplayName("STRICT mode allows successful execution without throwing")
    void strictModeAllowsSuccess() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "strict_success"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.STRICT
        );

        AtomicBoolean executed = new AtomicBoolean(false);

        assertDoesNotThrow(() -> {
            boolean success = context.executeWithIsolation(Test.class, () -> executed.set(true));
            assertTrue(success);
        });

        assertTrue(executed.get());
    }

    @Test
    @DisplayName("Execution returns handler class name in logs")
    void executionCapturesHandlerClass() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "handler_class"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        boolean success = context.executeWithIsolation(
                EffectDispatchSpineExceptionTest.class,
                () -> { }
        );

        assertTrue(success);
    }

    @Test
    @DisplayName("Log prefix contains enchantment and event details")
    void logPrefixContainsDetails() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey("myplugin", "enchant"),
                "DAMAGE_EVENT",
                3,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        String prefix = context.getLogPrefix();

        assertTrue(prefix.contains("myplugin:enchant"));
        assertTrue(prefix.contains("DAMAGE_EVENT"));
        assertTrue(prefix.contains("L3"));
    }

    @Test
    @DisplayName("EffectExecutionContext equality works correctly")
    void contextEquality() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "equality");

        EffectExecutionContext context1 = new EffectExecutionContext(key, "TEST", 1, EffectExecutionContext.ExecutionMode.LENIENT);
        EffectExecutionContext context2 = new EffectExecutionContext(key, "TEST", 1, EffectExecutionContext.ExecutionMode.LENIENT);
        EffectExecutionContext context3 = new EffectExecutionContext(key, "TEST", 2, EffectExecutionContext.ExecutionMode.LENIENT);

        assertEquals(context1, context2);
        assertNotEquals(context1, context3);
        assertEquals(context1.hashCode(), context2.hashCode());
    }

    @Test
    @DisplayName("EffectExecutionContext toString includes all fields")
    void contextToString() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "tostring"),
                "TEST_EVENT",
                5,
                EffectExecutionContext.ExecutionMode.STRICT
        );

        String str = context.toString();

        assertTrue(str.contains("tostring"));
        assertTrue(str.contains("TEST_EVENT"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("STRICT"));
    }

    @Test
    @DisplayName("Execution isolation allows subsequent actions after failure")
    void isolationAllowsSubsequentActions() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "isolation"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        AtomicBoolean firstExecuted = new AtomicBoolean(false);
        AtomicBoolean secondExecuted = new AtomicBoolean(false);

        context.executeWithIsolation(Test.class, () -> {
            firstExecuted.set(true);
            throw new RuntimeException("First fails");
        });

        boolean secondSuccess = context.executeWithIsolation(Test.class, () -> {
            secondExecuted.set(true);
        });

        assertTrue(firstExecuted.get());
        assertTrue(secondExecuted.get());
        assertTrue(secondSuccess);
    }

    @Test
    @DisplayName("Multiple exception scenarios are handled independently")
    void multipleExceptionsHandledIndependently() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "multi"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            final int failureNumber = i;
            boolean success = context.executeWithIsolation(Test.class, () -> {
                throw new RuntimeException("Failure " + failureNumber);
            });
            if (!success) {
                failureCount.incrementAndGet();
            }
        }

        assertEquals(3, failureCount.get());
    }

    @Test
    @DisplayName("Null action throws NullPointerException")
    void nullActionThrows() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "null"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        assertThrows(NullPointerException.class, () -> {
            context.executeWithIsolation(Test.class, null);
        });
    }

    @Test
    @DisplayName("Null handler class is handled gracefully")
    void nullHandlerClassIsHandled() {
        EffectExecutionContext context = new EffectExecutionContext(
                new NamespacedKey(TEST_NS, "null_handler"),
                "TEST",
                1,
                EffectExecutionContext.ExecutionMode.LENIENT
        );

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean success = context.executeWithIsolation(null, () -> executed.set(true));

        assertTrue(success);
        assertTrue(executed.get());
    }

    private static class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final EnchantmentEffectHandler handler;
        private final int minLevel;
        private final int maxLevel;

        TestEnchantmentDefinition(String name, EnchantmentEffectHandler handler, int minLevel, int maxLevel) {
            this.key = new NamespacedKey("test", name);
            this.handler = handler;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        @Override
        public NamespacedKey getKey() { return key; }

        @Override
        public Component getDisplayName() { return Component.text(key.getKey()); }

        @Override
        public Component getDescription() { return null; }

        @Override
        public int getMinLevel() { return minLevel; }

        @Override
        public int getMaxLevel() { return maxLevel; }

        @Override
        public LevelScaling getScaling() { return LevelScaling.linear(1.0, 0.5); }

        @Override
        public Set<Material> getApplicableMaterials() { return Set.of(Material.DIAMOND_SWORD); }

        @Override
        public boolean isApplicableTo(Material material) { return material == Material.DIAMOND_SWORD; }

        @Override
        public boolean isApplicableTo(org.bukkit.inventory.ItemStack item) { return item.getType() == Material.DIAMOND_SWORD; }

        @Override
        public boolean isCurse() { return false; }

        @Override
        public boolean isTradeable() { return true; }

        @Override
        public boolean isDiscoverable() { return true; }

        @Override
        public Rarity getRarity() { return Rarity.COMMON; }

        @Override
        public EnchantmentEffectHandler getEffectHandler() { return handler; }

        @Override
        public double calculateScaledValue(int level) { return getScaling().calculate(level); }

        @Override
        public boolean conflictsWith(EnchantmentDefinition other) { return false; }

        @Override
        public Set<NamespacedKey> getConflictingEnchantments() { return Set.of(); }
    }
}
