package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates enchantment offers for the enchantment table.
 * <p>
 * This class handles the generation of custom enchantment offers based on:
 * <ul>
 *   <li>Enchantment rarity weights (common, uncommon, rare, very rare)</li>
 *   <li>Table power level (number of bookshelves)</li>
 *   <li>Item material applicability</li>
 *   <li>Enchantment configuration (min/max bookshelves, cost multipliers)</li>
 *   <li>Conflict detection with vanilla and other custom enchantments</li>
 * </ul>
 * <p>
 * <strong>Offer Generation:</strong>
 * <ol>
 *   <li>Filter enchantments by discoverability and bookshelf range</li>
 *   <li>Calculate weighted probability for each eligible enchantment</li>
 *   <li>Randomly select enchantments for each slot</li>
 *   <li>Determine level based on power, slot position, and random factor</li>
 *   <li>Calculate cost based on level, rarity, and configuration</li>
 * </ol>
 *
 * @since 0.2.0
 */
public final class EnchantOfferGenerator {

    private static final int WEIGHT_COMMON = 10;
    private static final int WEIGHT_UNCOMMON = 5;
    private static final int WEIGHT_RARE = 2;
    private static final int WEIGHT_VERY_RARE = 1;

    private static final int MAX_POWER = 15;
    private static final int OFFER_SLOTS = 3;

    private final EnchantmentRegistryManager registryManager;

    /**
     * Creates a new offer generator.
     *
     * @param registryManager the enchantment registry for looking up definitions
     * @throws NullPointerException if registryManager is null
     * @since 0.2.0
     */
    public EnchantOfferGenerator(@NotNull EnchantmentRegistryManager registryManager) {
        this.registryManager = registryManager;
    }

    /**
     * Generates custom enchantment offers for an item.
     * <p>
     * Creates up to 3 offers based on the item type, table power, and
     * existing vanilla enchantments. Each offer includes the enchantment
     * definition, calculated level, cost, and configuration.
     *
     * @param item the item being enchanted
     * @param power the enchantment table power (number of bookshelves)
     * @param existingOffers set of vanilla enchantments already offered
     * @return list of generated custom offers (may be empty)
     * @since 0.2.0
     */
    @NotNull
    public List<GeneratedOffer> generateOffers(
            @NotNull ItemStack item,
            int power,
            @NotNull Set<Enchantment> existingOffers
    ) {
        if (item.getType().isAir()) {
            return Collections.emptyList();
        }

        Set<EnchantmentDefinition> candidates = getEligibleEnchantments(item, power, existingOffers);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<GeneratedOffer> offers = new ArrayList<>();
        Set<NamespacedKey> usedEnchantments = new HashSet<>();

        for (int slot = 0; slot < OFFER_SLOTS; slot++) {
            List<EnchantmentDefinition> available = filterAvailable(
                candidates, usedEnchantments, item
            );

            if (available.isEmpty()) {
                break;
            }

            GeneratedOffer offer = generateOfferForSlot(available, power, slot);
            if (offer != null) {
                offers.add(offer);

                if (!offer.configuration().allowsMultipleOffers()) {
                    usedEnchantments.add(offer.definition().getKey());
                }

                for (NamespacedKey conflict : offer.definition().getConflictingEnchantments()) {
                    usedEnchantments.add(conflict);
                }
            }
        }

        return offers;
    }

    /**
     * Gets all eligible enchantments for the given item and power level.
     * <p>
     * Filters enchantments by discoverability, bookshelf range, treasure status,
     * and conflicts with vanilla enchantments already in the offer array.
     *
     * @param item the item being enchanted
     * @param power the table power level
     * @param existingVanilla set of vanilla enchantments already offered
     * @return set of eligible custom enchantment definitions
     * @since 0.2.0
     */
    @NotNull
    private Set<EnchantmentDefinition> getEligibleEnchantments(
            @NotNull ItemStack item,
            int power,
            @NotNull Set<Enchantment> existingVanilla
    ) {
        Set<EnchantmentDefinition> candidates = new HashSet<>();

        for (EnchantmentDefinition definition : registryManager.getForMaterial(item.getType())) {
            if (!definition.isDiscoverable()) {
                continue;
            }

            EnchantTableConfiguration config = getTableConfiguration(definition);

            if (power < config.getMinBookshelves() || power > config.getMaxBookshelves()) {
                continue;
            }

            if (config.isTreasure()) {
                continue;
            }

            if (hasConflictWithVanilla(definition, existingVanilla)) {
                continue;
            }

            candidates.add(definition);
        }

        return candidates;
    }

    /**
     * Checks if an enchantment conflicts with any vanilla enchantments.
     *
     * @param definition the custom enchantment definition
     * @param vanillaOffers set of vanilla enchantments to check against
     * @return true if there's a conflict
     * @since 0.2.0
     */
    private boolean hasConflictWithVanilla(
            @NotNull EnchantmentDefinition definition,
            @NotNull Set<Enchantment> vanillaOffers
    ) {
        for (Enchantment vanilla : vanillaOffers) {
            NamespacedKey vanillaKey = vanilla.getKey();
            if (definition.getConflictingEnchantments().contains(vanillaKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filters available enchantments for the next slot.
     * <p>
     * Removes enchantments already used and those conflicting with used enchantments.
     * Also filters by applicability to the specific item.
     *
     * @param candidates the pool of eligible enchantments
     * @param used set of enchantment keys already used in previous slots
     * @param item the item being enchanted
     * @return list of available enchantments for this slot
     * @since 0.2.0
     */
    @NotNull
    private List<EnchantmentDefinition> filterAvailable(
            @NotNull Set<EnchantmentDefinition> candidates,
            @NotNull Set<NamespacedKey> used,
            @NotNull ItemStack item
    ) {
        List<EnchantmentDefinition> available = new ArrayList<>();

        for (EnchantmentDefinition definition : candidates) {
            if (used.contains(definition.getKey())) {
                continue;
            }

            if (!definition.isApplicableTo(item)) {
                continue;
            }

            boolean hasConflict = false;
            for (NamespacedKey usedKey : used) {
                if (definition.getConflictingEnchantments().contains(usedKey)) {
                    hasConflict = true;
                    break;
                }
            }

            if (!hasConflict) {
                available.add(definition);
            }
        }

        return available;
    }

    /**
     * Generates an offer for a specific slot.
     * <p>
     * Uses weighted random selection based on rarity weights. The selected
     * enchantment's level and cost are calculated based on power level and
     * slot position (higher slots generally offer better enchantments).
     *
     * @param available list of available enchantments for this slot
     * @param power the table power level
     * @param slotIndex the slot index (0-2)
     * @return the generated offer, or null if no offer could be generated
     * @since 0.2.0
     */
    private GeneratedOffer generateOfferForSlot(
            @NotNull List<EnchantmentDefinition> available,
            int power,
            int slotIndex
    ) {
        if (available.isEmpty()) {
            return null;
        }

        NavigableMap<Integer, EnchantmentDefinition> weightedMap = new TreeMap<>();
        int totalWeight = 0;

        for (EnchantmentDefinition definition : available) {
            int weight = calculateWeight(definition);
            totalWeight += weight;
            weightedMap.put(totalWeight, definition);
        }

        if (totalWeight == 0) {
            return null;
        }

        Random random = ThreadLocalRandom.current();
        int selection = random.nextInt(totalWeight) + 1;
        EnchantmentDefinition selected = weightedMap.ceilingEntry(selection).getValue();

        int level = determineLevel(selected, power, slotIndex);
        int cost = calculateCost(selected, level, power);

        EnchantTableConfiguration config = getTableConfiguration(selected);

        return new GeneratedOffer(selected, level, cost, config);
    }

    /**
     * Calculates the selection weight for an enchantment.
     * <p>
     * Base weights are: COMMON=10, UNCOMMON=5, RARE=2, VERY_RARE=1.
     * The weight multiplier from configuration is applied to adjust rarity.
     *
     * @param definition the enchantment definition
     * @return the calculated weight for random selection
     * @since 0.2.0
     */
    private int calculateWeight(@NotNull EnchantmentDefinition definition) {
        int baseWeight;
        switch (definition.getRarity()) {
            case COMMON -> baseWeight = WEIGHT_COMMON;
            case UNCOMMON -> baseWeight = WEIGHT_UNCOMMON;
            case RARE -> baseWeight = WEIGHT_RARE;
            case VERY_RARE -> baseWeight = WEIGHT_VERY_RARE;
            default -> baseWeight = WEIGHT_COMMON;
        }

        EnchantTableConfiguration config = getTableConfiguration(definition);
        return (int) (baseWeight * config.getWeightMultiplier());
    }

    /**
     * Determines the enchantment level for an offer.
     * <p>
     * Level is based on slot position (higher slots get better multipliers),
     * table power, and a random factor. Respects enchantment min/max levels
     * and table configuration limits.
     *
     * @param definition the enchantment definition
     * @param power the table power level
     * @param slotIndex the slot index (0-2)
     * @return the determined level
     * @since 0.2.0
     */
    private int determineLevel(
            @NotNull EnchantmentDefinition definition,
            int power,
            int slotIndex
    ) {
        EnchantTableConfiguration config = getTableConfiguration(definition);

        int minLevel = Math.max(definition.getMinLevel(), config.getTableMinLevel());
        int maxLevel = Math.min(definition.getMaxLevel(), config.getTableMaxLevel());

        int levelRange = maxLevel - minLevel + 1;

        double slotMultiplier;
        switch (slotIndex) {
            case 0 -> slotMultiplier = 0.3 + (Math.random() * 0.2);
            case 1 -> slotMultiplier = 0.5 + (Math.random() * 0.3);
            case 2 -> slotMultiplier = 0.7 + (Math.random() * 0.3);
            default -> slotMultiplier = 0.5;
        }

        double powerScale = Math.max(0.1, power / (double) MAX_POWER);
        int scaledMax = minLevel + (int) (levelRange * slotMultiplier * powerScale);

        return Math.max(minLevel, Math.min(maxLevel, scaledMax));
    }

    /**
     * Calculates the lapis/level cost for an enchantment offer.
     * <p>
     * Base cost depends on table power tier. Level multiplier adds 15% per level.
     * Cost multiplier from configuration is applied. Result is clamped to 1-30.
     *
     * @param definition the enchantment definition
     * @param level the offer level
     * @param power the table power level
     * @return the calculated cost in levels
     * @since 0.2.0
     */
    private int calculateCost(
            @NotNull EnchantmentDefinition definition,
            int level,
            int power
    ) {
        int baseCost;
        if (power <= 3) {
            baseCost = 5 + (int) (Math.random() * 3);
        } else if (power <= 7) {
            baseCost = 10 + (int) (Math.random() * 5);
        } else if (power <= 11) {
            baseCost = 15 + (int) (Math.random() * 5);
        } else {
            baseCost = 20 + (int) (Math.random() * 11);
        }

        double levelMultiplier = 1.0 + ((level - 1) * 0.15);
        int cost = (int) (baseCost * levelMultiplier);

        EnchantTableConfiguration config = getTableConfiguration(definition);
        cost = (int) (cost * config.getCostMultiplier());

        return Math.max(1, Math.min(30, cost));
    }

    /**
     * Gets the table configuration for an enchantment.
     * <p>
     * Currently returns default configuration. Future versions may support
     * per-enchantment custom configurations.
     *
     * @param definition the enchantment definition
     * @return the table configuration for this enchantment
     * @since 0.2.0
     */
    @NotNull
    private EnchantTableConfiguration getTableConfiguration(@NotNull EnchantmentDefinition definition) {
        return EnchantTableConfiguration.defaults();
    }

    /**
     * Record representing a generated enchantment offer.
     * <p>
     * Contains the enchantment definition, calculated level, cost, and
     * configuration used for this offer.
     *
     * @param definition the enchantment definition
     * @param level the generated level
     * @param cost the cost in experience levels
     * @param configuration the table configuration used
     * @since 0.2.0
     */
    public record GeneratedOffer(
            @NotNull EnchantmentDefinition definition,
            int level,
            int cost,
            @NotNull EnchantTableConfiguration configuration
    ) {
        /**
         * Converts this generated offer to a Bukkit EnchantmentOffer.
         *
         * @param enchantment the native Bukkit enchantment
         * @return the Bukkit enchantment offer
         * @since 0.2.0
         */
        @NotNull
        public org.bukkit.enchantments.EnchantmentOffer toBukkitOffer(@NotNull Enchantment enchantment) {
            return new org.bukkit.enchantments.EnchantmentOffer(enchantment, level, cost);
        }
    }
}
