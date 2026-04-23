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

public final class EnchantOfferGenerator {

    private static final int WEIGHT_COMMON = 10;
    private static final int WEIGHT_UNCOMMON = 5;
    private static final int WEIGHT_RARE = 2;
    private static final int WEIGHT_VERY_RARE = 1;

    private static final int MAX_POWER = 15;
    private static final int OFFER_SLOTS = 3;

    private final EnchantmentRegistryManager registryManager;

    public EnchantOfferGenerator(@NotNull EnchantmentRegistryManager registryManager) {
        this.registryManager = registryManager;
    }

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

    @NotNull
    private EnchantTableConfiguration getTableConfiguration(@NotNull EnchantmentDefinition definition) {
        return EnchantTableConfiguration.defaults();
    }

    public record GeneratedOffer(
            @NotNull EnchantmentDefinition definition,
            int level,
            int cost,
            @NotNull EnchantTableConfiguration configuration
    ) {
        @NotNull
        public org.bukkit.enchantments.EnchantmentOffer toBukkitOffer(@NotNull Enchantment enchantment) {
            return new org.bukkit.enchantments.EnchantmentOffer(enchantment, level, cost);
        }
    }
}
