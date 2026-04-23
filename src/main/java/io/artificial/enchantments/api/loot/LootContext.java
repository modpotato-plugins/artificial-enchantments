package io.artificial.enchantments.api.loot;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Context for loot modification during block break events.
 *
 * <p>This context provides all the data needed to modify loot drops when
 * a player breaks a block with an enchanted tool. The drops list is
 * modifiable, allowing enchantments to add, remove, or change items.
 *
 * <p><strong>Explicit Ownership:</strong><br>
 * Loot modification only occurs for enchantments that have explicitly
 * registered a {@link LootModifier} via {@link LootModifierRegistry}.
 * Non-targeted loot sources remain untouched unless explicitly opted in.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * LootModifier myModifier = context -> {
 *     // Get vanilla drops
 *     List<ItemStack> drops = context.getDrops();
 *
 *     // Add bonus item
 *     context.addDrop(new ItemStack(Material.DIAMOND));
 *
 *     // Double existing drops based on level
 *     int multiplier = 1 + context.getLevel();
 *     for (ItemStack drop : drops) {
 *         drop.setAmount(drop.getAmount() * multiplier);
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong><br>
 * Context instances are not thread-safe. Modifications should be done
 * synchronously on the main thread during the block break event.
 *
 * @see LootModifier
 * @see LootModifierRegistry
 * @since 0.4.0
 */
public interface LootContext {

    /**
     * Gets the tool item that triggered this loot modification.
     *
     * <p>This is the item the player is holding when breaking the block,
     * containing the enchantment that registered this modifier.
     *
     * @return the tool item stack
     * @since 0.4.0
     */
    @NotNull
    ItemStack getTool();

    /**
     * Gets the block being broken.
     *
     * @return the block
     * @since 0.4.0
     */
    @NotNull
    Block getBlock();

    /**
     * Gets the player breaking the block.
     *
     * @return the player
     * @since 0.4.0
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the location where drops will spawn.
     *
     * <p>Typically the block's location, but may be adjusted by other
     * enchantments or plugins.
     *
     * @return the drop location
     * @since 0.4.0
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the modifiable list of drops.
     *
     * <p>This list contains the vanilla drops that would normally spawn.
     * It is directly modifiable - changes are reflected in the actual
     * drops that will spawn.
     *
     * <p><strong>Modification Options:</strong>
     * <ul>
     *   <li>Add items: {@link #addDrop(ItemStack)}</li>
     *   <li>Remove items: {@link #removeDrop(ItemStack)}</li>
     *   <li>Clear all: {@link #clearDrops()}</li>
     *   <li>Replace all: {@link #setDrops(List)}</li>
     *   <li>Modify quantities: iterate and adjust amounts</li>
     * </ul>
     *
     * @return the modifiable drops list
     * @since 0.4.0
     */
    @NotNull
    List<ItemStack> getDrops();

    /**
     * Sets the entire drops list, replacing vanilla drops.
     *
     * <p>This replaces all drops. Use with care as it removes vanilla drops.
     * For adding bonus drops, prefer {@link #addDrop(ItemStack)}.
     *
     * @param drops the new drops list, or null to clear all drops
     * @since 0.4.0
     */
    void setDrops(@Nullable List<ItemStack> drops);

    /**
     * Adds a drop to the list.
     *
     * <p>This is the preferred method for adding bonus items. The item
     * is cloned before being added to prevent external modifications.
     *
     * @param drop the item to add
     * @throws IllegalArgumentException if drop is null
     * @since 0.4.0
     */
    void addDrop(@NotNull ItemStack drop);

    /**
     * Removes a drop from the list.
     *
     * <p>Removes the first matching item stack. The match is based on
     * material and amount comparison.
     *
     * @param drop the item to remove
     * @return true if an item was removed, false otherwise
     * @since 0.4.0
     */
    boolean removeDrop(@NotNull ItemStack drop);

    /**
     * Clears all drops from the list.
     *
     * <p>This removes vanilla drops. Use with caution.
     *
     * @since 0.4.0
     */
    void clearDrops();

    /**
     * Gets the enchantment level.
     *
     * <p>The level of the enchantment that registered this modifier.
     * Use this to scale modifications appropriately.
     *
     * @return the enchantment level (at least 1)
     * @since 0.4.0
     */
    int getLevel();

    /**
     * Gets the scaled value calculated from the enchantment level.
     *
     * <p>This is the pre-calculated value from the enchantment's
     * {@link io.artificial.enchantments.api.scaling.LevelScaling}.
     * Use this for consistent scaling across enchantments.
     *
     * @return the scaled value
     * @since 0.4.0
     */
    double getScaledValue();

    /**
     * Gets the experience points to drop.
     *
     * @return the XP amount
     * @since 0.4.0
     */
    int getExpToDrop();

    /**
     * Sets the experience points to drop.
     *
     * <p>Use this to adjust XP drops. Set to 0 to prevent XP drops.
     *
     * @param exp the XP amount (clamped to 0+)
     * @since 0.4.0
     */
    void setExpToDrop(int exp);

    /**
     * Checks if the tool has a specific enchantment.
     *
     * <p>Convenience method for checking if the tool has other enchantments
     * that might affect loot drops.
     *
     * @param enchantmentKey the enchantment key to check
     * @return true if the tool has this enchantment
     * @since 0.4.0
     */
    boolean hasEnchantment(@NotNull String enchantmentKey);

    /**
     * Gets the level of a specific enchantment on the tool.
     *
     * @param enchantmentKey the enchantment key to check
     * @return the enchantment level, or 0 if not present
     * @since 0.4.0
     */
    int getEnchantmentLevel(@NotNull String enchantmentKey);
}
