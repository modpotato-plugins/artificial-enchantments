package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Failure Case Tests")
class FailureCaseTest {

    private EnchantmentRegistryManager registry;
    private static final String TEST_NS = "test";

    @BeforeEach
    void setUp() {
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();
    }

    @Test
    @DisplayName("Register with null definition throws NullPointerException")
    void registerNullDefinitionThrows() {
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }

    @Test
    @DisplayName("Get with null key throws NullPointerException")
    void getWithNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> registry.get(null));
    }

    @Test
    @DisplayName("GetEnchantment with null key throws NullPointerException")
    void getEnchantmentWithNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> registry.getEnchantment(null));
    }

    @Test
    @DisplayName("Unregister with null key throws NullPointerException")
    void unregisterWithNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> registry.unregister(null));
    }

    @Test
    @DisplayName("GetForMaterial with null material throws NullPointerException")
    void getForMaterialWithNullThrows() {
        assertThrows(NullPointerException.class, () -> registry.getForMaterial(null));
    }

    @Test
    @DisplayName("Linear scaling with level 0 throws IllegalArgumentException")
    void linearScalingLevelZeroThrows() {
        LevelScaling scaling = LevelScaling.linear(1.0, 0.5);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, -100, Integer.MIN_VALUE})
    @DisplayName("Linear scaling with negative level throws IllegalArgumentException")
    void linearScalingNegativeLevelThrows(int level) {
        LevelScaling scaling = LevelScaling.linear(1.0, 0.5);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(level));
    }

    @Test
    @DisplayName("Exponential scaling with non-positive multiplier throws on construction")
    void exponentialScalingNonPositiveMultiplierThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.exponential(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.exponential(1.0, -1.0));
    }

    @Test
    @DisplayName("Exponential scaling with level 0 throws IllegalArgumentException")
    void exponentialScalingLevelZeroThrows() {
        LevelScaling scaling = LevelScaling.exponential(1.0, 1.5);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, -100})
    @DisplayName("Exponential scaling with negative level throws IllegalArgumentException")
    void exponentialScalingNegativeLevelThrows(int level) {
        LevelScaling scaling = LevelScaling.exponential(1.0, 1.5);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(level));
    }

    @Test
    @DisplayName("Diminishing scaling with non-positive maxValue throws")
    void diminishingScalingNonPositiveMaxThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.diminishing(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.diminishing(-1.0, 1.0));
    }

    @Test
    @DisplayName("Diminishing scaling with non-positive scalingFactor throws")
    void diminishingScalingNonPositiveFactorThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.diminishing(10.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> LevelScaling.diminishing(10.0, -1.0));
    }

    @Test
    @DisplayName("Diminishing scaling with level 0 throws IllegalArgumentException")
    void diminishingScalingLevelZeroThrows() {
        LevelScaling scaling = LevelScaling.diminishing(10.0, 5.0);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, -100})
    @DisplayName("Diminishing scaling with negative level throws IllegalArgumentException")
    void diminishingScalingNegativeLevelThrows(int level) {
        LevelScaling scaling = LevelScaling.diminishing(10.0, 5.0);
        assertThrows(IllegalArgumentException.class, () -> scaling.calculate(level));
    }

    @Test
    @DisplayName("Unregister non-existing enchantment returns false")
    void unregisterNonExistingReturnsFalse() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "never_existed");
        boolean result = registry.unregister(key);
        assertFalse(result);
    }

    @Test
    @DisplayName("Get non-existing enchantment returns empty optional")
    void getNonExistingReturnsEmpty() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "does_not_exist");
        var result = registry.get(key);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GetEnchantment for non-existing returns null")
    void getEnchantmentNonExistingReturnsNull() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "not_there");
        EnchantmentDefinition result = registry.getEnchantment(key);
        assertNull(result);
    }

    @Test
    @DisplayName("Material index returns empty set for material with no enchantments")
    void materialIndexEmptyForNoEnchantments() {
        Set<EnchantmentDefinition> result = registry.getForMaterial(Material.BAMBOO);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Registry clear on empty registry succeeds")
    void clearEmptyRegistrySucceeds() {
        assertDoesNotThrow(() -> registry.clear());
        assertTrue(registry.getAll().isEmpty());
    }

    @Test
    @DisplayName("Double clear succeeds")
    void doubleClearSucceeds() {
        registry.register(createTestEnchantment("clear_me"));
        registry.clear();
        assertDoesNotThrow(() -> registry.clear());
        assertTrue(registry.getAll().isEmpty());
    }

    @Test
    @DisplayName("MarkNativeRegistered for non-existing key marks it as native registered")
    void markNativeRegisteredNonExistingMarksIt() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "not_registered");
        assertFalse(registry.isNativeRegistered(key));
        assertDoesNotThrow(() -> registry.markNativeRegistered(key));
        assertTrue(registry.isNativeRegistered(key));
    }

    @Test
    @DisplayName("IsNativeRegistered for non-existing key returns false")
    void isNativeRegisteredNonExistingReturnsFalse() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "never_seen");
        assertFalse(registry.isNativeRegistered(key));
    }

    @Test
    @DisplayName("GetPendingRegistrations returns empty when nothing pending")
    void getPendingRegistrationsWhenEmpty() {
        var pending = registry.getPendingRegistrations();
        assertTrue(pending.isEmpty());
    }

    @Test
    @DisplayName("Enchantment with empty materials set is handled")
    void enchantmentWithEmptyMaterials() {
        EnchantmentDefinition enchantment = new TestEnchantmentDefinition("no_materials", Set.of());
        boolean result = registry.register(enchantment);
        assertTrue(result);
        
        Set<EnchantmentDefinition> forSword = registry.getForMaterial(Material.DIAMOND_SWORD);
        assertFalse(forSword.contains(enchantment));
    }

    @Test
    @DisplayName("Multiple unregisters of same enchantment only first succeeds")
    void multipleUnregistersOnlyFirstSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("multi_unregister");
        registry.register(enchantment);
        
        boolean first = registry.unregister(enchantment.getKey());
        boolean second = registry.unregister(enchantment.getKey());
        boolean third = registry.unregister(enchantment.getKey());
        
        assertTrue(first);
        assertFalse(second);
        assertFalse(third);
    }

    private EnchantmentDefinition createTestEnchantment(String name) {
        return createTestEnchantment(name, Material.DIAMOND_SWORD);
    }

    private EnchantmentDefinition createTestEnchantment(String name, Material material) {
        return new TestEnchantmentDefinition(name, Set.of(material));
    }

    private static class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final Set<Material> materials;

        TestEnchantmentDefinition(String name, Set<Material> materials) {
            this.key = new NamespacedKey("test", name);
            this.materials = materials;
        }

        @Override
        public NamespacedKey getKey() { return key; }

        @Override
        public Component getDisplayName() { return Component.text(key.getKey()); }

        @Override
        public Component getDescription() { return null; }

        @Override
        public int getMinLevel() { return 1; }

        @Override
        public int getMaxLevel() { return 5; }

        @Override
        public LevelScaling getScaling() { return LevelScaling.linear(1.0, 0.5); }

        @Override
        public Set<Material> getApplicableMaterials() { return materials; }

        @Override
        public boolean isApplicableTo(Material material) { return materials.contains(material); }

        @Override
        public boolean isApplicableTo(org.bukkit.inventory.ItemStack item) { return materials.contains(item.getType()); }

        @Override
        public boolean isCurse() { return false; }

        @Override
        public boolean isTradeable() { return true; }

        @Override
        public boolean isDiscoverable() { return true; }

        @Override
        public Rarity getRarity() { return Rarity.COMMON; }

        @Override
        public EnchantmentEffectHandler getEffectHandler() { return null; }

        @Override
        public double calculateScaledValue(int level) { return getScaling().calculate(level); }

        @Override
        public boolean conflictsWith(EnchantmentDefinition other) { return false; }

        @Override
        public Set<NamespacedKey> getConflictingEnchantments() { return Set.of(); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnchantmentDefinition that)) return false;
            return key.equals(that.getKey());
        }

        @Override
        public int hashCode() { return key.hashCode(); }
    }
}
