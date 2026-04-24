package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for item-related enchantment effects.
 *
 * <p>Provides access to player, item, equipment slot, durability, and
 * item action data during durability changes, drops, pickups, and
 * crafting/anvil operations.
 *
 * @since 0.1.0
 */
public interface ItemContext extends EffectContext {

    /**
     * Gets the player holding the item.
     *
     * @return the player, or null for environmental item events
     */
    @Nullable
    Player getPlayer();

    /**
     * Gets the item being processed.
     *
     * @return the item
     */
    @NotNull
    ItemStack getItem();

    /**
     * Gets the equipment slot where the item is held.
     *
     * @return the slot, or null if not applicable
     */
    @Nullable
    EquipmentSlot getSlot();

    /**
     * Gets the location of the item.
     *
     * @return the item location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the current durability of the item.
     *
     * @return current durability value
     */
    int getCurrentDurability();

    /**
     * Gets the maximum durability of the item.
     *
     * @return maximum durability value
     */
    int getMaxDurability();

    /**
     * Gets the damage being taken by this item.
     *
     * @return the damage amount
     */
    int getDamageTaken();

    /**
     * Sets the damage to be taken by this item.
     *
     * @param damage the new damage amount
     */
    void setDamageTaken(int damage);

    /**
     * Reduces the damage taken by a specified amount.
     *
     * @param reduction the amount to reduce
     */
    void reduceDamage(int reduction);

    /**
     * Checks if the item will break from this damage.
     *
     * @return true if item will break
     */
    boolean willBreak();

    /**
     * Checks if this event is an item drop.
     *
     * @return true if dropping
     */
    boolean isDrop();

    /**
     * Checks if this event is an item pickup.
     *
     * @return true if picking up
     */
    boolean isPickup();

    /**
     * Checks if the item is being used as a crafting ingredient.
     *
     * @return true if in crafting
     */
    boolean isCraftingIngredient();

    /**
     * Checks if the item is being combined in an anvil.
     *
     * @return true if in anvil
     */
    boolean isAnvilCombination();
}
