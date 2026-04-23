package io.artificial.enchantments.internal.loot;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.loot.LootContext;
import io.artificial.enchantments.api.loot.LootModifier;
import io.artificial.enchantments.internal.ItemEnchantmentService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LootModifierRegistryImpl Tests")
class LootModifierRegistryImplTest {

    private LootModifierRegistryImpl registry;
    private static final String TEST_NS = "test";

    @BeforeEach
    void setUp() {
        registry = new LootModifierRegistryImpl();
    }

    @Test
    @DisplayName("Registry creation succeeds")
    void registryCreationSucceeds() {
        assertNotNull(registry);
        assertEquals(0, registry.getEnchantmentCount());
        assertEquals(0, registry.getTotalModifierCount());
    }

    @Test
    @DisplayName("Register modifier for enchantment succeeds")
    void registerModifierSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier = context -> {};

        registry.register(enchantment, modifier);

        assertTrue(registry.hasModifier(enchantment));
        assertEquals(1, registry.getModifierCount(enchantment));
        assertTrue(registry.isRegistered(enchantment, modifier));
    }

    @Test
    @DisplayName("Register multiple modifiers for same enchantment succeeds")
    void registerMultipleModifiersSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier1 = context -> {};
        LootModifier modifier2 = context -> {};

        registry.register(enchantment, modifier1);
        registry.register(enchantment, modifier2);

        assertTrue(registry.hasModifier(enchantment));
        assertEquals(2, registry.getModifierCount(enchantment));
        assertTrue(registry.isRegistered(enchantment, modifier1));
        assertTrue(registry.isRegistered(enchantment, modifier2));
    }

    @Test
    @DisplayName("Duplicate modifier registration is idempotent")
    void duplicateRegistrationIsIdempotent() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier = context -> {};

        registry.register(enchantment, modifier);
        registry.register(enchantment, modifier);

        assertEquals(1, registry.getModifierCount(enchantment));
    }

    @Test
    @DisplayName("Unregister specific modifier succeeds")
    void unregisterSpecificModifierSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier = context -> {};

        registry.register(enchantment, modifier);
        boolean removed = registry.unregister(enchantment, modifier);

        assertTrue(removed);
        assertFalse(registry.hasModifier(enchantment));
        assertEquals(0, registry.getModifierCount(enchantment));
    }

    @Test
    @DisplayName("Unregister non-existent modifier returns false")
    void unregisterNonExistentModifierReturnsFalse() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier = context -> {};

        boolean removed = registry.unregister(enchantment, modifier);

        assertFalse(removed);
    }

    @Test
    @DisplayName("Unregister all modifiers for enchantment succeeds")
    void unregisterAllModifiersSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier1 = context -> {};
        LootModifier modifier2 = context -> {};

        registry.register(enchantment, modifier1);
        registry.register(enchantment, modifier2);
        boolean removed = registry.unregisterAll(enchantment);

        assertTrue(removed);
        assertFalse(registry.hasModifier(enchantment));
    }

    @Test
    @DisplayName("Get modifiers returns list in registration order")
    void getModifiersReturnsListInOrder() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier1 = context -> { context.addDrop(new ItemStack(Material.DIAMOND)); };
        LootModifier modifier2 = context -> { context.addDrop(new ItemStack(Material.EMERALD)); };

        registry.register(enchantment, modifier1);
        registry.register(enchantment, modifier2);

        List<LootModifier> modifiers = registry.getModifiers(enchantment);

        assertEquals(2, modifiers.size());
        assertSame(modifier1, modifiers.get(0));
        assertSame(modifier2, modifiers.get(1));
    }

    @Test
    @DisplayName("Get modifiers for enchantment with no modifiers returns empty list")
    void getModifiersForEmptyEnchantmentReturnsEmptyList() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);

        List<LootModifier> modifiers = registry.getModifiers(enchantment);

        assertNotNull(modifiers);
        assertTrue(modifiers.isEmpty());
    }

    @Test
    @DisplayName("Has modifier returns false for enchantment with no modifiers")
    void hasModifierReturnsFalseForEmptyEnchantment() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);

        assertFalse(registry.hasModifier(enchantment));
    }

    @Test
    @DisplayName("Clear removes all registrations")
    void clearRemovesAllRegistrations() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test_enchant1", 1, 5);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test_enchant2", 1, 3);

        registry.register(enchantment1, context -> {});
        registry.register(enchantment2, context -> {});

        registry.clear();

        assertEquals(0, registry.getEnchantmentCount());
        assertEquals(0, registry.getTotalModifierCount());
        assertFalse(registry.hasModifier(enchantment1));
        assertFalse(registry.hasModifier(enchantment2));
    }

    @Test
    @DisplayName("Register with null enchantment throws exception")
    void registerWithNullEnchantmentThrows() {
        LootModifier modifier = context -> {};

        assertThrows(IllegalArgumentException.class, () -> registry.register(null, modifier));
    }

    @Test
    @DisplayName("Register with null modifier throws exception")
    void registerWithNullModifierThrows() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);

        assertThrows(IllegalArgumentException.class, () -> registry.register(enchantment, null));
    }

    @Test
    @DisplayName("Is registered returns false for non-registered modifier")
    void isRegisteredReturnsFalseForNonRegistered() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", 1, 5);
        LootModifier modifier = context -> {};

        assertFalse(registry.isRegistered(enchantment, modifier));
    }

    @Test
    @DisplayName("Multiple enchantments can have different modifiers")
    void multipleEnchantmentsCanHaveDifferentModifiers() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test_enchant1", 1, 5);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test_enchant2", 1, 3);
        LootModifier modifier1 = context -> {};
        LootModifier modifier2 = context -> {};

        registry.register(enchantment1, modifier1);
        registry.register(enchantment2, modifier2);

        assertTrue(registry.hasModifier(enchantment1));
        assertTrue(registry.hasModifier(enchantment2));
        assertTrue(registry.isRegistered(enchantment1, modifier1));
        assertTrue(registry.isRegistered(enchantment2, modifier2));
        assertFalse(registry.isRegistered(enchantment1, modifier2));
        assertFalse(registry.isRegistered(enchantment2, modifier1));
    }

    private EnchantmentDefinition createTestEnchantment(String name, int minLevel, int maxLevel) {
        return new TestEnchantmentDefinition(new NamespacedKey(TEST_NS, name), minLevel, maxLevel);
    }

    private static class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final int minLevel;
        private final int maxLevel;

        TestEnchantmentDefinition(NamespacedKey key, int minLevel, int maxLevel) {
            this.key = key;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        @Override
        public NamespacedKey getKey() { return key; }

        @Override
        public net.kyori.adventure.text.Component getDisplayName() {
            return net.kyori.adventure.text.Component.text(key.getKey());
        }

        @Override
        public net.kyori.adventure.text.Component getDescription() {
            return net.kyori.adventure.text.Component.empty();
        }

        @Override
        public int getMinLevel() { return minLevel; }

        @Override
        public int getMaxLevel() { return maxLevel; }

        @Override
        public io.artificial.enchantments.api.scaling.LevelScaling getScaling() {
            return io.artificial.enchantments.api.scaling.LevelScaling.linear(1.0, 0.5);
        }

        @Override
        public double calculateScaledValue(int level) { return 1.0 + (level - 1) * 0.5; }

        @Override
        public java.util.Set<org.bukkit.Material> getApplicableMaterials() {
            return java.util.Collections.emptySet();
        }

        @Override
        public boolean isApplicableTo(org.bukkit.Material material) { return false; }

        @Override
        public boolean isApplicableTo(@NotNull org.bukkit.inventory.ItemStack item) {
            return false;
        }

        @Override
        public Rarity getRarity() { return Rarity.COMMON; }

        @Override
        public boolean isCurse() { return false; }

        @Override
        public boolean isTradeable() { return true; }

        @Override
        public boolean isDiscoverable() { return true; }

        @Override
        public io.artificial.enchantments.api.EnchantmentEffectHandler getEffectHandler() { return null; }

        @Override
        public java.util.Set<org.bukkit.NamespacedKey> getConflictingEnchantments() {
            return java.util.Collections.emptySet();
        }

        @Override
        public boolean conflictsWith(@NotNull EnchantmentDefinition other) {
            return false;
        }
    }
}
