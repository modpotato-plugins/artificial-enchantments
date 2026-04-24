package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an item-related enchantment effect triggers.
 *
 * <p>Carries data about the player, item, durability, and action type during
 * item drop, pickup, or durability change events. This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
public class ItemEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final EquipmentSlot slot;
    private final boolean isDrop;
    private final boolean isPickup;
    private final int currentDurability;
    private final int maxDurability;
    private int damageTaken;
    private boolean cancelled;

    /**
     * Creates a new {@code ItemEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player associated with the item
     * @param item the item
     * @param slot the equipment slot
     * @param isDrop true if this is a drop event
     * @param isPickup true if this is a pickup event
     * @param currentDurability the current durability
     * @param maxDurability the maximum durability
     * @param damageTaken the damage taken
     */
    public ItemEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull EquipmentSlot slot,
            boolean isDrop,
            boolean isPickup,
            int currentDurability,
            int maxDurability,
            int damageTaken
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.item = item;
        this.slot = slot;
        this.isDrop = isDrop;
        this.isPickup = isPickup;
        this.currentDurability = currentDurability;
        this.maxDurability = maxDurability;
        this.damageTaken = damageTaken;
        this.cancelled = false;
    }

    /**
     * Gets the player associated with the item.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the item.
     *
     * @return the item
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the equipment slot.
     *
     * @return the slot
     */
    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    /**
     * Checks if this is a drop event.
     *
     * @return true if drop
     */
    public boolean isDrop() {
        return isDrop;
    }

    /**
     * Checks if this is a pickup event.
     *
     * @return true if pickup
     */
    public boolean isPickup() {
        return isPickup;
    }

    /**
     * Gets the current durability.
     *
     * @return the current durability
     */
    public int getCurrentDurability() {
        return currentDurability;
    }

    /**
     * Gets the maximum durability.
     *
     * @return the maximum durability
     */
    public int getMaxDurability() {
        return maxDurability;
    }

    /**
     * Gets the damage taken.
     *
     * @return the damage taken
     */
    public int getDamageTaken() {
        return damageTaken;
    }

    /**
     * Sets the damage taken.
     *
     * @param damageTaken the new damage taken (clamped to non-negative)
     */
    public void setDamageTaken(int damageTaken) {
        this.damageTaken = Math.max(0, damageTaken);
    }

    /**
     * Reduces the damage taken by the specified amount.
     *
     * @param amount the amount to reduce
     */
    public void reduceDamage(int amount) {
        this.damageTaken = Math.max(0, this.damageTaken - amount);
    }

    /**
     * Checks if the item will break from the damage.
     *
     * @return true if the item will break
     */
    public boolean willBreak() {
        return currentDurability - damageTaken <= 0;
    }

    /**
     * Checks if this is a crafting ingredient.
     *
     * @return true if crafting ingredient
     */
    public boolean isCraftingIngredient() {
        return false;
    }

    /**
     * Checks if this is an anvil combination.
     *
     * @return true if anvil combination
     */
    public boolean isAnvilCombination() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
