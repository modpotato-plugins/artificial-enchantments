package io.artificial.enchantments.internal.anvil;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Logic for combining enchantments in an anvil.
 * <p>
 * Handles book+item and book+book combinations with conflict detection,
 * level merging, and cost calculation.
 *
 * @since 0.2.0
 */
public final class AnvilCombinationLogic {

    private final ItemStorage itemStorage;

    /**
     * Creates a new anvil combination logic instance.
     *
     * @param itemStorage the item storage for enchantment operations
     */
    public AnvilCombinationLogic(@NotNull ItemStorage itemStorage) {
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
    }

    /**
     * Result of an anvil combination operation.
     *
     * @param result the resulting item stack, or null if too expensive or no change
     * @param cost the total anvil cost in experience levels
     * @param newPriorWork the new prior work penalty for the result item
     * @param tooExpensive whether the combination exceeds the maximum allowed cost
     * @param appliedEnchantments map of enchantments applied to the result
     * @param conflicts set of enchantments that conflicted and were not applied
     */
    public record CombinationResult(
            @Nullable ItemStack result,
            int cost,
            int newPriorWork,
            boolean tooExpensive,
            Map<EnchantmentDefinition, Integer> appliedEnchantments,
            Set<EnchantmentDefinition> conflicts
    ) {
        /**
         * Returns whether the combination was successful.
         *
         * @return true if a result item was produced and the cost was acceptable
         */
        public boolean isSuccess() {
            return result != null && !tooExpensive;
        }
    }

    /**
     * Attempts to combine a target item with a sacrifice (book or item).
     * <p>
     * This handles all anvil combination logic:
     * <ul>
     *   <li>Conflict detection between enchantments</li>
     *   <li>Level merging (taking max level when both have same enchant)</li>
     *   <li>Cost calculation</li>
     *   <li>Too expensive detection</li>
     * </ul>
     *
     * @param target the target item being modified
     * @param sacrifice the sacrifice item (book or item providing enchantments)
     * @param targetPriorWork prior work penalty of target item
     * @param sacrificePriorWork prior work penalty of sacrifice item
     * @param isTreasureBook whether sacrifice is a treasure book
     * @return the combination result
     * @since 0.2.0
     */
    @NotNull
    public CombinationResult combine(
            @NotNull ItemStack target,
            @NotNull ItemStack sacrifice,
            int targetPriorWork,
            int sacrificePriorWork,
            boolean isTreasureBook) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(sacrifice, "sacrifice cannot be null");

        if (target.getType().isAir() || sacrifice.getType().isAir()) {
            return emptyResult();
        }

        Map<EnchantmentDefinition, Integer> targetEnchantments = getEnchantments(target);
        Map<EnchantmentDefinition, Integer> sacrificeEnchantments = getEnchantments(sacrifice);

        if (sacrificeEnchantments.isEmpty() && !isEnchantedBook(sacrifice)) {
            return emptyResult();
        }

        Map<EnchantmentDefinition, Integer> applicableEnchantments = new HashMap<>();
        Map<EnchantmentDefinition, Integer> conflicts = new HashMap<>();
        int totalCost = 0;

        for (Map.Entry<EnchantmentDefinition, Integer> entry : sacrificeEnchantments.entrySet()) {
            EnchantmentDefinition enchantment = entry.getKey();
            int sacrificeLevel = entry.getValue();
            int targetLevel = targetEnchantments.getOrDefault(enchantment, 0);

            if (hasConflicts(enchantment, targetEnchantments, applicableEnchantments.keySet())) {
                conflicts.put(enchantment, sacrificeLevel);
                continue;
            }

            if (!enchantment.isApplicableTo(target.getType())) {
                if (!isEnchantedBook(sacrifice)) {
                    continue;
                }
            }

            int resultLevel = calculateMergedLevel(targetLevel, sacrificeLevel, enchantment.getMaxLevel());

            if (resultLevel > targetLevel) {
                applicableEnchantments.put(enchantment, resultLevel);
                int enchantCost = calculateEnchantmentCost(enchantment, resultLevel, targetLevel, isTreasureBook);
                totalCost += enchantCost;
            }
        }

        if (applicableEnchantments.isEmpty() && conflicts.isEmpty()) {
            return emptyResult();
        }

        totalCost += targetPriorWork + sacrificePriorWork + AnvilCostCalculator.BASE_COMBINE_COST;
        boolean tooExpensive = AnvilCostCalculator.isTooExpensive(totalCost);

        ItemStack result = null;
        if (!tooExpensive) {
            result = buildResultItem(target, targetEnchantments, applicableEnchantments);
        }

        int newPriorWork = AnvilCostCalculator.calculateNewPriorWork(
                AnvilCostCalculator.maxPriorWork(targetPriorWork, sacrificePriorWork)
        );

        return new CombinationResult(
                result,
                totalCost,
                newPriorWork,
                tooExpensive,
                applicableEnchantments,
                conflicts.keySet()
        );
    }

    /**
     * Attempts to combine two enchanted books.
     * <p>
     * Book+book combination allows merging enchantments from both books,
     * taking the maximum level for each enchantment. Unlike item+book,
     * books can hold incompatible enchantments.
     *
     * @param bookA first enchanted book
     * @param bookB second enchanted book
     * @param priorWorkA prior work of first book
     * @param priorWorkB prior work of second book
     * @return the combination result
     * @since 0.2.0
     */
    @NotNull
    public CombinationResult combineBooks(
            @NotNull ItemStack bookA,
            @NotNull ItemStack bookB,
            int priorWorkA,
            int priorWorkB) {
        Objects.requireNonNull(bookA, "bookA cannot be null");
        Objects.requireNonNull(bookB, "bookB cannot be null");

        if (!isEnchantedBook(bookA) || !isEnchantedBook(bookB)) {
            return emptyResult();
        }

        Map<EnchantmentDefinition, Integer> enchantmentsA = getEnchantments(bookA);
        Map<EnchantmentDefinition, Integer> enchantmentsB = getEnchantments(bookB);

        Map<EnchantmentDefinition, Integer> resultEnchantments = new HashMap<>();
        Map<EnchantmentDefinition, Integer> conflicts = new HashMap<>();
        int totalCost = AnvilCostCalculator.BASE_COMBINE_COST;

        resultEnchantments.putAll(enchantmentsA);

        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantmentsB.entrySet()) {
            EnchantmentDefinition enchantment = entry.getKey();
            int levelB = entry.getValue();
            int levelA = resultEnchantments.getOrDefault(enchantment, 0);

            if (hasConflictsWithSet(enchantment, resultEnchantments.keySet())) {
                conflicts.put(enchantment, levelB);
                continue;
            }

            int resultLevel = calculateMergedLevel(levelA, levelB, enchantment.getMaxLevel());
            resultEnchantments.put(enchantment, resultLevel);

            if (resultLevel > levelA) {
                int enchantCost = calculateEnchantmentCost(enchantment, resultLevel, levelA, false);
                totalCost += enchantCost;
            }
        }

        totalCost += priorWorkA + priorWorkB;
        boolean tooExpensive = AnvilCostCalculator.isTooExpensive(totalCost);

        ItemStack result = null;
        if (!tooExpensive) {
            result = buildBookResult(resultEnchantments);
        }

        int newPriorWork = AnvilCostCalculator.calculateNewPriorWork(
                AnvilCostCalculator.maxPriorWork(priorWorkA, priorWorkB)
        );

        return new CombinationResult(
                result,
                totalCost,
                newPriorWork,
                tooExpensive,
                resultEnchantments,
                conflicts.keySet()
        );
    }

    @NotNull
    private Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item) {
        return itemStorage.getEnchantments(item);
    }

    private boolean isEnchantedBook(@NotNull ItemStack item) {
        return item.getType() == Material.ENCHANTED_BOOK;
    }

    private boolean hasConflicts(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull Map<EnchantmentDefinition, Integer> existingEnchantments,
            @NotNull Set<EnchantmentDefinition> pendingEnchantments) {
        Set<NamespacedKey> conflicts = enchantment.getConflictingEnchantments();

        for (EnchantmentDefinition existing : existingEnchantments.keySet()) {
            if (conflicts.contains(existing.getKey()) || existing.conflictsWith(enchantment)) {
                return true;
            }
        }

        for (EnchantmentDefinition pending : pendingEnchantments) {
            if (conflicts.contains(pending.getKey()) || pending.conflictsWith(enchantment)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasConflictsWithSet(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull Set<EnchantmentDefinition> enchantments) {
        Set<NamespacedKey> conflicts = enchantment.getConflictingEnchantments();

        for (EnchantmentDefinition existing : enchantments) {
            if (conflicts.contains(existing.getKey()) || existing.conflictsWith(enchantment)) {
                return true;
            }
        }

        return false;
    }

    private int calculateMergedLevel(int levelA, int levelB, int maxLevel) {
        if (levelA == levelB && levelA < maxLevel) {
            return levelA + 1;
        }
        return Math.max(levelA, levelB);
    }

    private int calculateEnchantmentCost(
            EnchantmentDefinition enchantment,
            int resultLevel,
            int originalLevel,
            boolean isTreasureBook) {
        int rarityMultiplier = getRarityMultiplier(enchantment.getRarity());
        int baseCost = rarityMultiplier * resultLevel;

        if (resultLevel > originalLevel) {
            int upgradeLevels = resultLevel - originalLevel;
            baseCost += rarityMultiplier * upgradeLevels;
        }

        if (isTreasureBook) {
            baseCost *= 2;
        }

        return baseCost;
    }

    private int getRarityMultiplier(EnchantmentDefinition.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    @NotNull
    private ItemStack buildResultItem(
            @NotNull ItemStack target,
            @NotNull Map<EnchantmentDefinition, Integer> originalEnchantments,
            @NotNull Map<EnchantmentDefinition, Integer> newEnchantments) {
        ItemStack result = target.clone();
        for (Map.Entry<EnchantmentDefinition, Integer> entry : newEnchantments.entrySet()) {
            result = itemStorage.applyEnchantment(result, entry.getKey(), entry.getValue());
        }
        return result;
    }

    @NotNull
    private ItemStack buildBookResult(@NotNull Map<EnchantmentDefinition, Integer> enchantments) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            book = itemStorage.applyEnchantment(book, entry.getKey(), entry.getValue());
        }

        return book;
    }

    @NotNull
    private CombinationResult emptyResult() {
        return new CombinationResult(
                null,
                0,
                0,
                false,
                Collections.emptyMap(),
                Collections.emptySet()
        );
    }
}
