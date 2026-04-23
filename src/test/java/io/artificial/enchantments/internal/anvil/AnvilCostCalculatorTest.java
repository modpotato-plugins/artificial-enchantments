package io.artificial.enchantments.internal.anvil;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnvilCostCalculator Tests")
class AnvilCostCalculatorTest {

    private static final NamespacedKey TEST_KEY = new NamespacedKey("test", "enchant");

    private EnchantmentDefinition createTestEnchantment(EnchantmentDefinition.Rarity rarity, int maxLevel) {
        return EnchantmentDefinition.builder()
                .key(TEST_KEY)
                .displayName(Component.text("Test Enchant"))
                .minLevel(1)
                .maxLevel(maxLevel)
                .scaling(LevelScaling.linear(1.0, 0.5))
                .applicable(Material.DIAMOND_SWORD)
                .rarity(rarity)
                .build();
    }

    @Test
    @DisplayName("Calculate combine cost with no enchantments returns base cost")
    void combineCostWithNoEnchantments() {
        Map<EnchantmentDefinition, Integer> targetEnchantments = Collections.emptyMap();
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = Collections.emptyMap();

        int cost = AnvilCostCalculator.calculateCombineCost(
                targetEnchantments, sacrificeEnchantments, 0, 0, false
        );

        assertEquals(2, cost);
    }

    @ParameterizedTest
    @CsvSource({
            "COMMON, 1, 1",
            "COMMON, 5, 5",
            "UNCOMMON, 1, 2",
            "UNCOMMON, 5, 10",
            "RARE, 1, 4",
            "RARE, 5, 20",
            "VERY_RARE, 1, 8",
            "VERY_RARE, 5, 37"
    })
    @DisplayName("Enchantment cost scales with rarity and level")
    void enchantmentCostScalesWithRarityAndLevel(EnchantmentDefinition.Rarity rarity, int level, int expectedCost) {
        EnchantmentDefinition enchantment = createTestEnchantment(rarity, 5);
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = new HashMap<>();
        sacrificeEnchantments.put(enchantment, level);

        int cost = AnvilCostCalculator.calculateCombineCost(
                Collections.emptyMap(), sacrificeEnchantments, 0, 0, false
        );

        assertEquals(2 + expectedCost, cost);
    }

    @Test
    @DisplayName("Treasure books double the cost")
    void treasureBooksDoubleCost() {
        EnchantmentDefinition enchantment = createTestEnchantment(EnchantmentDefinition.Rarity.COMMON, 5);
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = new HashMap<>();
        sacrificeEnchantments.put(enchantment, 3);

        int normalCost = AnvilCostCalculator.calculateCombineCost(
                Collections.emptyMap(), sacrificeEnchantments, 0, 0, false
        );

        int treasureCost = AnvilCostCalculator.calculateCombineCost(
                Collections.emptyMap(), sacrificeEnchantments, 0, 0, true
        );

        assertEquals(treasureCost, normalCost * 2 - 2);
    }

    @Test
    @DisplayName("Prior work penalties are added to cost")
    void priorWorkPenaltiesAdded() {
        EnchantmentDefinition enchantment = createTestEnchantment(EnchantmentDefinition.Rarity.COMMON, 5);
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = new HashMap<>();
        sacrificeEnchantments.put(enchantment, 1);

        int cost = AnvilCostCalculator.calculateCombineCost(
                Collections.emptyMap(), sacrificeEnchantments, 3, 5, false
        );

        assertEquals(2 + 1 + 3 + 5, cost);
    }

    @Test
    @DisplayName("Cost is capped at maximum (39)")
    void costIsCapped() {
        EnchantmentDefinition enchantment = createTestEnchantment(EnchantmentDefinition.Rarity.VERY_RARE, 5);
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = new HashMap<>();
        sacrificeEnchantments.put(enchantment, 5);

        int cost = AnvilCostCalculator.calculateCombineCost(
                Collections.emptyMap(), sacrificeEnchantments, 10, 10, false
        );

        assertEquals(39, cost);
    }

    @Test
    @DisplayName("Calculate rename cost returns expected value")
    void renameCostCalculation() {
        int cost = AnvilCostCalculator.calculateRenameCost(null, "New Name", 0);
        assertEquals(1, cost);

        cost = AnvilCostCalculator.calculateRenameCost(null, "New Name", 3);
        assertEquals(4, cost);
    }

    @Test
    @DisplayName("New prior work is calculated correctly")
    void newPriorWorkCalculation() {
        assertEquals(1, AnvilCostCalculator.calculateNewPriorWork(0));
        assertEquals(3, AnvilCostCalculator.calculateNewPriorWork(1));
        assertEquals(7, AnvilCostCalculator.calculateNewPriorWork(3));
        assertEquals(15, AnvilCostCalculator.calculateNewPriorWork(7));
    }

    @Test
    @DisplayName("Max prior work returns maximum of two values")
    void maxPriorWork() {
        assertEquals(5, AnvilCostCalculator.maxPriorWork(3, 5));
        assertEquals(5, AnvilCostCalculator.maxPriorWork(5, 3));
        assertEquals(5, AnvilCostCalculator.maxPriorWork(5, 5));
    }

    @Test
    @DisplayName("Get max cost returns 39")
    void getMaxCost() {
        assertEquals(39, AnvilCostCalculator.getMaxCost());
    }

    @Test
    @DisplayName("Is too expensive returns true for costs above max")
    void isTooExpensive() {
        assertFalse(AnvilCostCalculator.isTooExpensive(0));
        assertFalse(AnvilCostCalculator.isTooExpensive(39));
        assertTrue(AnvilCostCalculator.isTooExpensive(40));
        assertTrue(AnvilCostCalculator.isTooExpensive(100));
    }
}
