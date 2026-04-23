package io.artificial.enchantments.internal.loot;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.loot.LootModifier;
import io.artificial.enchantments.api.loot.LootModifierRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe implementation of {@link LootModifierRegistry}.
 *
 * <p>This implementation uses concurrent collections to ensure thread safety
 * for all registry operations. The registry maintains a map of enchantments
 * to their registered modifiers, with each enchantment having a list of
 * modifiers that are invoked in registration order.
 *
 * <p><strong>Thread Safety:</strong>
 * <ul>
 *   <li>Registration is thread-safe</li>
 *   <li>Read operations are lock-free</li>
 *   <li>Modifier lists use CopyOnWriteArrayList for safe iteration</li>
 *   <li>All operations are non-blocking</li>
 * </ul>
 *
 * @since 0.4.0
 */
public final class LootModifierRegistryImpl implements LootModifierRegistry {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    // Map of enchantment key -> list of modifiers
    // Using ConcurrentHashMap for thread-safe read/write
    // Using CopyOnWriteArrayList for safe iteration during modification
    private final Map<String, List<LootModifier>> modifiers;

    /**
     * Creates a new empty registry.
     */
    public LootModifierRegistryImpl() {
        this.modifiers = new ConcurrentHashMap<>();
    }

    @Override
    public void register(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier) {
        if (enchantment == null) {
            throw new IllegalArgumentException("enchantment cannot be null");
        }
        if (modifier == null) {
            throw new IllegalArgumentException("modifier cannot be null");
        }

        String key = enchantment.getKey().toString();

        modifiers.compute(key, (k, existingList) -> {
            if (existingList == null) {
                // Create new list with this modifier
                List<LootModifier> newList = new CopyOnWriteArrayList<>();
                newList.add(modifier);
                LOGGER.fine("Registered loot modifier for " + key);
                return newList;
            } else {
                // Check for duplicates (identity comparison)
                for (LootModifier existing : existingList) {
                    if (existing == modifier) {
                        LOGGER.fine("Modifier already registered for " + key + ", ignoring duplicate");
                        return existingList;
                    }
                }
                // Add to existing list
                existingList.add(modifier);
                LOGGER.fine("Added additional loot modifier for " + key);
                return existingList;
            }
        });
    }

    @Override
    public boolean unregister(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        Objects.requireNonNull(modifier, "modifier cannot be null");

        String key = enchantment.getKey().toString();

        boolean[] removed = {false};
        modifiers.compute(key, (k, existingList) -> {
            if (existingList == null) {
                return null;
            }
            removed[0] = existingList.removeIf(m -> m == modifier);
            if (removed[0]) {
                LOGGER.fine("Unregistered loot modifier from " + key);
            }
            // Remove the key if list is now empty
            return existingList.isEmpty() ? null : existingList;
        });
        return removed[0];
    }

    @Override
    public boolean unregisterAll(@NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        String key = enchantment.getKey().toString();
        List<LootModifier> removed = modifiers.remove(key);

        if (removed != null && !removed.isEmpty()) {
            LOGGER.fine("Unregistered all " + removed.size() + " loot modifiers from " + key);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull List<LootModifier> getModifiers(@NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        String key = enchantment.getKey().toString();
        List<LootModifier> list = modifiers.get(key);

        if (list == null) {
            return Collections.emptyList();
        }

        // Return unmodifiable view to prevent external modification
        return Collections.unmodifiableList(list);
    }

    @Override
    public boolean hasModifier(@NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        String key = enchantment.getKey().toString();
        List<LootModifier> list = modifiers.get(key);
        return list != null && !list.isEmpty();
    }

    @Override
    public int getModifierCount(@NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        String key = enchantment.getKey().toString();
        List<LootModifier> list = modifiers.get(key);
        return list != null ? list.size() : 0;
    }

    @Override
    public void clear() {
        int totalEnchantments = modifiers.size();
        int totalModifiers = modifiers.values().stream()
                .mapToInt(List::size)
                .sum();

        modifiers.clear();

        LOGGER.info("Cleared all loot modifiers: " + totalModifiers + " modifiers from "
                + totalEnchantments + " enchantments");
    }

    @Override
    public boolean isRegistered(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        Objects.requireNonNull(modifier, "modifier cannot be null");

        String key = enchantment.getKey().toString();
        List<LootModifier> list = modifiers.get(key);

        if (list == null) {
            return false;
        }

        // Identity comparison
        for (LootModifier m : list) {
            if (m == modifier) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the total number of enchantments with registered modifiers.
     *
     * @return the count of enchantments
     */
    public int getEnchantmentCount() {
        return modifiers.size();
    }

    /**
     * Gets the total number of registered modifiers across all enchantments.
     *
     * @return the total modifier count
     */
    public int getTotalModifierCount() {
        return modifiers.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
