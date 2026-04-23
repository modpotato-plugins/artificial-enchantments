package io.artificial.enchantments.api.loot;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Registry for managing loot modifiers per-enchantment.
 *
 * <p>This registry provides explicit registration of {@link LootModifier} instances
 * for specific enchantments. Only enchantments with registered modifiers can affect
 * loot drops - this ensures clear ownership boundaries and prevents implicit
 * modification of non-targeted loot sources.
 *
 * <p><strong>Explicit Ownership Model:</strong>
 * <ul>
 *   <li>Plugins decide which enchantments affect which loot sources</li>
 *   <li>No automatic modification without explicit opt-in registration</li>
 *   <li>Non-targeted loot remains untouched unless explicitly opted in</li>
 *   <li>Multiple modifiers can be registered for the same enchantment</li>
 *   <li>Registration is idempotent (duplicate modifiers are ignored)</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Get the registry from the API
 * ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.getInstance();
 * LootModifierRegistry registry = api.getLootModifierRegistry();
 *
 * // Create a modifier
 * LootModifier fortuneLike = context -> {
 *     int bonus = context.getLevel() * 0.5; // 50% bonus per level
 *     for (ItemStack drop : context.getDrops()) {
 *         drop.setAmount(drop.getAmount() + bonus);
 *     }
 * };
 *
 * // Register for your enchantment
 * registry.register(myEnchantment, fortuneLike);
 *
 * // Check if enchantment has modifiers
 * if (registry.hasModifier(myEnchantment)) {
 *     List<LootModifier> modifiers = registry.getModifiers(myEnchantment);
 * }
 *
 * // Unregister when needed
 * registry.unregister(myEnchantment, fortuneLike);
 * }</pre>
 *
 * <p><strong>Extension Points:</strong><br>
 * This registry manages block-break loot modifiers. Future extensions can add:
 * <ul>
 *   <li>{@code EntityLootModifierRegistry} - for entity death drops</li>
 *   <li>{@code FishingLootModifierRegistry} - for fishing rewards</li>
 *   <li>{@code ContainerLootModifierRegistry} - for container loot</li>
 * </ul>
 * Each follows the same pattern of explicit registration and clear ownership.
 *
 * @see LootModifier
 * @see LootContext
 * @since 0.4.0
 */
public interface LootModifierRegistry {

    /**
     * Registers a loot modifier for an enchantment.
     *
     * <p>The modifier will be invoked whenever a player breaks a block with
     * a tool containing this enchantment. Multiple modifiers can be registered
     * for the same enchantment - they will all be invoked in registration order.
     *
     * <p>Registration is idempotent - registering the same modifier twice
     * for the same enchantment has no effect.
     *
     * @param enchantment the enchantment to register the modifier for
     * @param modifier the modifier to register
     * @throws IllegalArgumentException if enchantment or modifier is null
     * @since 0.4.0
     */
    void register(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier);

    /**
     * Unregisters a specific loot modifier from an enchantment.
     *
     * <p>Removes a previously registered modifier. If the modifier was not
     * registered for this enchantment, this method does nothing.
     *
     * @param enchantment the enchantment to remove the modifier from
     * @param modifier the modifier to unregister
     * @return true if the modifier was found and removed, false otherwise
     * @throws IllegalArgumentException if enchantment or modifier is null
     * @since 0.4.0
     */
    boolean unregister(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier);

    /**
     * Unregisters all loot modifiers for an enchantment.
     *
     * <p>Removes all modifiers for the specified enchantment. After this call,
     * {@link #hasModifier(EnchantmentDefinition)} will return false.
     *
     * @param enchantment the enchantment to clear modifiers for
     * @return true if any modifiers were removed, false if none existed
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.4.0
     */
    boolean unregisterAll(@NotNull EnchantmentDefinition enchantment);

    /**
     * Gets all loot modifiers registered for an enchantment.
     *
     * <p>Returns the list of modifiers in registration order. The returned
     * list is unmodifiable - use {@link #register} and {@link #unregister}
     * to modify registrations.
     *
     * @param enchantment the enchantment to get modifiers for
     * @return list of registered modifiers (may be empty, never null)
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.4.0
     */
    @NotNull
    List<LootModifier> getModifiers(@NotNull EnchantmentDefinition enchantment);

    /**
     * Checks if an enchantment has any registered loot modifiers.
     *
     * <p>This is the primary API for checking if an enchantment affects loot drops.
     * Use this before processing loot modifications.
     *
     * @param enchantment the enchantment to check
     * @return true if at least one modifier is registered
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.4.0
     */
    boolean hasModifier(@NotNull EnchantmentDefinition enchantment);

    /**
     * Gets the number of modifiers registered for an enchantment.
     *
     * @param enchantment the enchantment to check
     * @return the number of registered modifiers (0 if none)
     * @throws IllegalArgumentException if enchantment is null
     * @since 0.4.0
     */
    int getModifierCount(@NotNull EnchantmentDefinition enchantment);

    /**
     * Clears all registrations from this registry.
     *
     * <p><strong>Warning:</strong> This removes ALL loot modifier registrations
     * from ALL enchantments. This is primarily for cleanup during shutdown.
     *
     * @since 0.4.0
     */
    void clear();

    /**
     * Checks if a specific modifier is registered for an enchantment.
     *
     * @param enchantment the enchantment to check
     * @param modifier the modifier to look for
     * @return true if the modifier is registered for this enchantment
     * @throws IllegalArgumentException if enchantment or modifier is null
     * @since 0.4.0
     */
    boolean isRegistered(@NotNull EnchantmentDefinition enchantment, @NotNull LootModifier modifier);
}
