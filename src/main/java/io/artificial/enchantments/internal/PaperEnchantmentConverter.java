package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts library EnchantmentDefinition instances to Paper's native
 * EnchantmentRegistryEntry.Builder format for registration during bootstrap.
 * 
 * <p>This utility class bridges the library's enchantment definition format with
 * Paper 1.21+'s native registry system. It handles conversion of properties
 * like description, rarity, costs, supported items, and exclusive conflicts.
 *
 * @since 0.2.0
 */
public final class PaperEnchantmentConverter {

    /**
     * Private constructor to prevent instantiation of utility class.
     *
     * @since 0.2.0
     */
    private PaperEnchantmentConverter() {
    }

    /**
     * Configures a Paper EnchantmentRegistryEntry.Builder from an EnchantmentDefinition.
     *
     * @param definition the library enchantment definition
     * @param builder the Paper builder to configure
     * @param event the registry compose event for tag lookups
     */
    public static void convertToBuilder(
            @NotNull EnchantmentDefinition definition,
            @NotNull EnchantmentRegistryEntry.Builder builder,
            @NotNull RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event) {

        builder.description(definition.getDisplayName());
        builder.maxLevel(definition.getMaxLevel());
        builder.weight(convertRarityToWeight(definition.getRarity()));
        builder.anvilCost(1 + definition.getMaxLevel() / 2);
        builder.activeSlots(EquipmentSlotGroup.ANY);

        RegistryKeySet<ItemType> supportedItems = determineSupportedItems(definition);
        builder.supportedItems(supportedItems);
        builder.primaryItems(supportedItems);

        builder.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                1,
                (int) definition.calculateScaledValue(1)
        ));
        builder.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                definition.getMaxLevel(),
                (int) definition.calculateScaledValue(definition.getMaxLevel())
        ));

        if (!definition.getConflictingEnchantments().isEmpty()) {
            Set<TypedKey<Enchantment>> conflictKeys = definition.getConflictingEnchantments()
                    .stream()
                    .map(key -> EnchantmentKeys.create(Key.key(key.getNamespace(), key.getKey())))
                    .collect(Collectors.toSet());
            builder.exclusiveWith(RegistrySet.keySet(RegistryKey.ENCHANTMENT, conflictKeys));
        }
    }

    private static int convertRarityToWeight(@NotNull EnchantmentDefinition.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 10;
            case UNCOMMON -> 5;
            case RARE -> 2;
            case VERY_RARE -> 1;
        };
    }

    private static RegistryKeySet<ItemType> determineSupportedItems(@NotNull EnchantmentDefinition definition) {
        Set<org.bukkit.Material> materials = definition.getApplicableMaterials();

        if (materials.isEmpty()) {
            return RegistrySet.keySet(RegistryKey.ITEM);
        }

        Set<Key> itemKeys = materials.stream()
                .map(material -> Key.key(material.getKey().getNamespace(), material.getKey().getKey()))
                .collect(Collectors.toSet());

        try {
            return RegistrySet.keySetFromValues(RegistryKey.ITEM,
                    itemKeys.stream()
                            .map(key -> org.bukkit.Registry.ITEM.get(key))
                            .filter(item -> item != null)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            return RegistrySet.keySet(RegistryKey.ITEM);
        }
    }
}
