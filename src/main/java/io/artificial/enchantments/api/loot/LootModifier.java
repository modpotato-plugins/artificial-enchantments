package io.artificial.enchantments.api.loot;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for modifying loot drops from enchanted items.
 *
 * <p>Loot modifiers are explicitly registered per-enchantment via
 * {@link LootModifierRegistry}. Only enchantments with registered modifiers
 * can affect drops - this ensures clear ownership and prevents implicit
 * modification of non-targeted loot sources.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Define a modifier that doubles drops
 * LootModifier fortuneModifier = context -> {
 *     int level = context.getLevel();
 *     List<ItemStack> drops = context.getDrops();
 *
 *     // Modify quantities
 *     for (ItemStack drop : drops) {
 *         drop.setAmount(drop.getAmount() * (1 + level));
 *     }
 * };
 *
 * // Register for your enchantment
 * LootModifierRegistry registry = api.getLootModifierRegistry();
 * registry.register(myEnchantment, fortuneModifier);
 * }</pre>
 *
 * <p><strong>Extension Points:</strong><br>
 * This system is designed for extension to other loot sources:
 * <ul>
 *   <li>Entity death loot (entity loot modifiers)</li>
 *   <li>Fishing rewards (fishing loot modifiers)</li>
 *   <li>Container loot (chest/barrel modifiers)</li>
 *   <li>Block interaction loot (non-break sources)</li>
 * </ul>
 * Future extensions should follow the same pattern:
 * explicit registration, clear ownership, modifiable context.
 *
 * <p><strong>Ownership Model:</strong><br>
 * - Plugins decide which enchantments affect which loot sources<br>
 * - No automatic modification without explicit opt-in<br>
 * - Non-targeted loot remains untouched<br>
 * - Multiple modifiers can stack on same enchantment
 *
 * @see LootContext
 * @see LootModifierRegistry
 * @since 0.4.0
 */
@FunctionalInterface
public interface LootModifier {

    /**
     * Modifies the loot drops based on the context.
     *
     * <p>This method is called when a block is broken by a tool with an
     * enchantment that has registered loot modifiers. The context provides
     * access to:
     * <ul>
     *   <li>The tool item with the enchantment</li>
     *   <li>The block being broken</li>
     *   <li>The player breaking the block</li>
     *   <li>The modifiable drops list</li>
     *   <li>The enchantment level</li>
     * </ul>
     *
     * <p>The drops list is directly modifiable - you can:
     * <ul>
     *   <li>Add new items with {@link LootContext#addDrop(ItemStack)}</li>
     *   <li>Remove items with {@link LootContext#removeDrop(ItemStack)}</li>
     *   <li>Modify quantities of existing items</li>
     *   <li>Replace the entire drops list with {@link LootContext#setDrops(List)}</li>
     * </ul>
     *
     * <p><strong>Important:</strong> This method should not perform heavy
     * operations. Keep modifications lightweight and synchronous. If you need
     * to do expensive work, schedule it via the scheduler.
     *
     * @param context the loot context containing all relevant data and the modifiable drops list
     * @since 0.4.0
     */
    void modify(@NotNull LootContext context);
}
