package io.artificial.enchantments.internal.anvil;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Calculates anvil costs for enchantment operations.
 * <p>
 * Implements vanilla-like anvil cost calculation with support for custom
 * enchantments. Costs are based on:
 * <ul>
 *   <li>Base operation cost (repair, combine, rename)</li>
 *   <li>Per-enchantment cost based on rarity and level</li>
 *   <li>Prior work penalty from previous anvil uses</li>
 *   <li>Treasure enchantment multiplier</li>
 * </ul>
 *
 * @since 0.2.0
 */
public final class AnvilCostCalculator {

    private static final int MAX_COST = 39;
    private static final int RENAME_COST = 1;
    private static final int BASE_REPAIR_COST = 2;
    static final int BASE_COMBINE_COST = 2;

    private static final int TREASURE_MULTIPLIER = 2;

    private static final int COMMON_MULTIPLIER = 1;
    private static final int UNCOMMON_MULTIPLIER = 2;
    private static final int RARE_MULTIPLIER = 4;
    private static final int VERY_RARE_MULTIPLIER = 8;

    private AnvilCostCalculator() {
    }

    /**
     * Calculates the cost for combining two items in an anvil.
     * <p>
     * This includes costs for:
     * <ul>
     *   <li>Base combine operation</li>
     *   <li>Each enchantment being added/upgraded</li>
     *   <li>Prior work penalties from both items</li>
     * </ul>
     *
     * @param targetEnchantments enchantments on the target item
     * @param sacrificeEnchantments enchantments on the sacrifice item (book or item)
     * @param targetPriorWork prior work penalty of target item
     * @param sacrificePriorWork prior work penalty of sacrifice item
     * @param isTreasureBook whether the sacrifice is a treasure book
     * @return the total experience cost
     * @since 0.2.0
     */
    public static int calculateCombineCost(
            @NotNull Map<EnchantmentDefinition, Integer> targetEnchantments,
            @NotNull Map<EnchantmentDefinition, Integer> sacrificeEnchantments,
            int targetPriorWork,
            int sacrificePriorWork,
            boolean isTreasureBook) {
        Objects.requireNonNull(targetEnchantments, "targetEnchantments cannot be null");
        Objects.requireNonNull(sacrificeEnchantments, "sacrificeEnchantments cannot be null");

        int cost = BASE_COMBINE_COST;
        for (Map.Entry<EnchantmentDefinition, Integer> entry : sacrificeEnchantments.entrySet()) {
            EnchantmentDefinition enchantment = entry.getKey();
            int sacrificeLevel = entry.getValue();
            int targetLevel = targetEnchantments.getOrDefault(enchantment, 0);

            int enchantCost = calculateEnchantmentCost(enchantment, sacrificeLevel, targetLevel, isTreasureBook);
            cost += enchantCost;
        }
        cost += targetPriorWork + sacrificePriorWork;

        return Math.min(cost, MAX_COST);
    }

    /**
     * Calculates the cost for renaming an item.
     *
     * @param currentName the current name of the item (null if unnamed)
     * @param newName the desired new name (null to clear name)
     * @param priorWork prior work penalty of the item
     * @return the experience cost, or -1 if too expensive
     * @since 0.2.0
     */
    public static int calculateRenameCost(String currentName, String newName, int priorWork) {
        int cost = RENAME_COST + priorWork;
        return Math.min(cost, MAX_COST);
    }

    /**
     * Calculates the cost for repairing an item with materials.
     *
     * @param materialCost cost of materials used
     * @param priorWork prior work penalty of the item
     * @return the total experience cost
     * @since 0.2.0
     */
    public static int calculateRepairCost(int materialCost, int priorWork) {
        int cost = BASE_REPAIR_COST + materialCost + priorWork;
        return Math.min(cost, MAX_COST);
    }

    /**
     * Calculates the new prior work penalty after an anvil operation.
     * <p>
     * Prior work penalty doubles each time an item is used in an anvil,
     * capped at a reasonable maximum to prevent exponential growth.
     *
     * @param currentPriorWork the current prior work penalty
     * @return the new prior work penalty
     * @since 0.2.0
     */
    public static int calculateNewPriorWork(int currentPriorWork) {
        int newPriorWork = currentPriorWork * 2 + 1;
        return Math.min(newPriorWork, 31);
    }

    /**
     * Calculates the maximum of two prior work penalties.
     * <p>
     * Used when combining items - the result gets the higher of the two penalties.
     *
     * @param priorWorkA first prior work value
     * @param priorWorkB second prior work value
     * @return the maximum prior work
     * @since 0.2.0
     */
    public static int maxPriorWork(int priorWorkA, int priorWorkB) {
        return Math.max(priorWorkA, priorWorkB);
    }

    /**
     * Checks if a cost exceeds the maximum allowed (too expensive).
     *
     * @param cost the calculated cost
     * @return true if the cost exceeds the maximum
     * @since 0.2.0
     */
    public static boolean isTooExpensive(int cost) {
        return cost > MAX_COST;
    }

    /**
     * Gets the maximum allowed anvil cost.
     *
     * @return the maximum cost (39)
     * @since 0.2.0
     */
    public static int getMaxCost() {
        return MAX_COST;
    }

    private static int calculateEnchantmentCost(
            EnchantmentDefinition enchantment,
            int sacrificeLevel,
            int targetLevel,
            boolean isTreasureBook) {
        int rarityMultiplier = getRarityMultiplier(enchantment.getRarity());
        int baseCost = rarityMultiplier * sacrificeLevel;

        if (sacrificeLevel > targetLevel && targetLevel > 0) {
            int upgradeLevels = sacrificeLevel - targetLevel;
            baseCost += rarityMultiplier * upgradeLevels;
        }

        if (isTreasureBook) {
            baseCost *= TREASURE_MULTIPLIER;
        }

        return baseCost;
    }

    private static int getRarityMultiplier(EnchantmentDefinition.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> COMMON_MULTIPLIER;
            case UNCOMMON -> UNCOMMON_MULTIPLIER;
            case RARE -> RARE_MULTIPLIER;
            case VERY_RARE -> VERY_RARE_MULTIPLIER;
        };
    }
}
